package com.zufar.icedlatte.supportchat.service;

import static com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus.FAILED;
import static com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus.SENT;
import static com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType.CUSTOMER;
import static com.zufar.icedlatte.supportchat.entity.SupportMessageSenderType.OWNER;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.zufar.icedlatte.security.api.dto.CurrentUserSnapshot;
import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageDeliveryStatus;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;
import com.zufar.icedlatte.supportchat.exception.SupportChatConversationNotFoundException;
import com.zufar.icedlatte.supportchat.owner.OwnerMessage;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageDeliveryResult;
import com.zufar.icedlatte.supportchat.owner.OwnerMessageSender;
import com.zufar.icedlatte.supportchat.realtime.SupportChatMessagePublisher;
import com.zufar.icedlatte.supportchat.repository.SupportConversationRepository;
import com.zufar.icedlatte.supportchat.repository.SupportMessageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
class SupportChatMessageFlowService {

    private final SupportChatAvailabilityService availabilityService;
    private final SupportConversationRepository conversationRepository;
    private final SupportMessageRepository messageRepository;
    private final SupportChatMessageBodyPolicy messageBodyPolicy;
    private final SupportChatCustomerMessagePolicy customerMessagePolicy;
    private final OwnerMessageSender ownerMessageSender;
    private final SupportChatMessagePublisher messagePublisher;
    private final TransactionTemplate writeTransactionTemplate;

    SupportChatMessageFlowService(
            SupportChatAvailabilityService availabilityService,
            SupportConversationRepository conversationRepository,
            SupportMessageRepository messageRepository,
            SupportChatMessageBodyPolicy messageBodyPolicy,
            SupportChatCustomerMessagePolicy customerMessagePolicy,
            OwnerMessageSender ownerMessageSender,
            SupportChatMessagePublisher messagePublisher,
            PlatformTransactionManager transactionManager) {
        this.availabilityService = availabilityService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.messageBodyPolicy = messageBodyPolicy;
        this.customerMessagePolicy = customerMessagePolicy;
        this.ownerMessageSender = ownerMessageSender;
        this.messagePublisher = messagePublisher;
        this.writeTransactionTemplate = createTransactionTemplate(transactionManager);
    }

    public SupportMessageEntity sendCustomerMessage(
            CurrentUserSnapshot user,
            UUID conversationId,
            UUID clientMessageId,
            String body,
            String turnstileToken,
            String clientIp) {
        PendingCustomerMessage pendingMessage = writeTransactionTemplate.execute(
                _ -> prepareCustomerMessage(user, conversationId, clientMessageId, body, turnstileToken, clientIp));
        Objects.requireNonNull(pendingMessage, "pendingMessage");

        SupportMessageEntity message = pendingMessage.message();
        if (pendingMessage.alreadyAccepted()) {
            return message;
        }

        SupportConversationEntity conversation = pendingMessage.conversation();
        var deliveryResult = sendToOwner(conversation, message, user);
        boolean delivered = deliveryResult.delivered();
        SupportMessageEntity updatedMessage = markCustomerDeliveryStatus(message, delivered);
        log.info(
                "support_chat.customer_message.accepted: conversationId={}, messageId={}, ownerDelivered={}",
                conversation.getId(),
                updatedMessage.getId(),
                delivered);
        return updatedMessage;
    }

    @Transactional
    public Optional<SupportMessageEntity> saveOwnerReply(
            SupportConversationEntity conversation, String body, long telegramUpdateId, long telegramMessageId) {
        UUID conversationId = conversation.getId();
        if (messageRepository.existsByTelegramUpdateId(telegramUpdateId)) {
            log.info(
                    "support_chat.owner_reply.duplicate_ignored: conversationId={}, telegramUpdateId={}",
                    conversationId,
                    telegramUpdateId);
            return Optional.empty();
        }

        var messageContent = messageBodyPolicy.normalizeAndValidate(body);
        SupportMessageEntity saved = messageRepository.save(
                createOwnerReply(conversationId, messageContent, telegramUpdateId, telegramMessageId));
        conversationRepository.touchLastMessageAt(conversationId);
        messagePublisher.publishOwnerReply(conversation, saved);

        log.info(
                "support_chat.owner_reply.accepted: conversationId={}, messageId={}, telegramUpdateId={}",
                conversationId,
                saved.getId(),
                telegramUpdateId);
        return Optional.of(saved);
    }

    private PendingCustomerMessage prepareCustomerMessage(
            CurrentUserSnapshot user,
            UUID conversationId,
            UUID clientMessageId,
            String body,
            String turnstileToken,
            String clientIp) {
        availabilityService.requireAvailable(user);
        UUID userId = user.id();
        SupportConversationEntity conversation = ensureOwnsConversation(userId, conversationId);
        var messageContent = messageBodyPolicy.normalizeAndValidate(body);

        var existing = messageRepository.findByConversationIdAndClientMessageId(conversationId, clientMessageId);
        if (existing.isPresent()) {
            return new PendingCustomerMessage(conversation, existing.get(), true);
        }

        SupportMessageEntity previousCustomerMessage = messageRepository
                .findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(conversationId, CUSTOMER)
                .orElse(null);
        customerMessagePolicy.enforceCustomerMessageRules(
                userId, conversationId, clientIp, messageContent, previousCustomerMessage, turnstileToken);

        SupportMessageEntity saved = messageRepository.save(
                createCustomerMessage(conversation.getId(), userId, clientMessageId, messageContent));
        conversationRepository.touchLastMessageAt(conversation.getId());
        return new PendingCustomerMessage(conversation, saved, false);
    }

    private SupportMessageEntity markCustomerDeliveryStatus(SupportMessageEntity message, boolean delivered) {
        SupportMessageDeliveryStatus deliveryStatus = delivered ? SENT : FAILED;
        writeTransactionTemplate.executeWithoutResult(_ -> {
            int updatedRows = messageRepository.updateDeliveryStatus(message.getId(), deliveryStatus, !delivered);
            if (updatedRows != 1) {
                throw new IllegalStateException("Support chat message delivery status was not updated");
            }
        });
        message.setDeliveryStatus(deliveryStatus);
        message.setOperatorInspectionRequired(!delivered);
        return message;
    }

    private SupportConversationEntity ensureOwnsConversation(UUID userId, UUID conversationId) {
        return conversationRepository
                .findById(conversationId)
                .filter(conversation -> conversation.getUserId().equals(userId))
                .orElseThrow(SupportChatConversationNotFoundException::new);
    }

    private OwnerMessageDeliveryResult sendToOwner(
            SupportConversationEntity conversation, SupportMessageEntity saved, CurrentUserSnapshot user) {
        UUID id = conversation.getId();
        UUID messageId = saved.getId();
        try {
            OwnerMessage ownerMessage =
                    new OwnerMessage(id, messageId, user.displayName(), user.email(), saved.getBody());
            return ownerMessageSender.send(ownerMessage);
        } catch (RuntimeException ex) {
            log.warn(
                    "support_chat.owner_message.delivery_failed: conversationId={}, messageId={}, exceptionClass={}",
                    id,
                    messageId,
                    ex.getClass().getSimpleName());
            return OwnerMessageDeliveryResult.failedResult();
        }
    }

    private static SupportMessageEntity createOwnerReply(
            UUID conversationId,
            SupportChatMessageBodyPolicy.MessageContent messageContent,
            long telegramUpdateId,
            long telegramMessageId) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setConversationId(conversationId);
        message.setSenderType(OWNER);
        message.setBody(messageContent.body());
        message.setNormalizedBody(messageContent.duplicateKey());
        message.setDeliveryStatus(SENT);
        message.setTelegramUpdateId(telegramUpdateId);
        message.setTelegramMessageId(telegramMessageId);
        return message;
    }

    private static SupportMessageEntity createCustomerMessage(
            UUID conversationId,
            UUID userId,
            UUID clientMessageId,
            SupportChatMessageBodyPolicy.MessageContent messageContent) {
        SupportMessageEntity message = new SupportMessageEntity();
        message.setConversationId(conversationId);
        message.setSenderType(CUSTOMER);
        message.setSenderUserId(userId);
        message.setClientMessageId(clientMessageId);
        message.setBody(messageContent.body());
        message.setNormalizedBody(messageContent.duplicateKey());
        return message;
    }

    private static TransactionTemplate createTransactionTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(false);
        return transactionTemplate;
    }

    private record PendingCustomerMessage(
            SupportConversationEntity conversation, SupportMessageEntity message, boolean alreadyAccepted) {}
}
