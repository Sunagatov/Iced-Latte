-- liquibase formatted sql

-- changeset zufar:create-support-chat-conversations
CREATE TABLE public.support_conversations
(
    id                         UUID                     NOT NULL PRIMARY KEY,
    user_id                    UUID                     NOT NULL,
    created_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
    last_message_at            TIMESTAMP WITH TIME ZONE,
    telegram_message_thread_id BIGINT,
    telegram_fallback_message_id BIGINT,

    CONSTRAINT uq_support_conversations_user_id UNIQUE (user_id),
    CONSTRAINT fk_support_conversations_user
        FOREIGN KEY (user_id)
            REFERENCES public.user_details (id)
            ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_support_conversations_telegram_thread
    ON public.support_conversations (telegram_message_thread_id)
    WHERE telegram_message_thread_id IS NOT NULL;

CREATE INDEX idx_support_conversations_telegram_fallback_message
    ON public.support_conversations (telegram_fallback_message_id)
    WHERE telegram_fallback_message_id IS NOT NULL;

-- changeset zufar:create-support-chat-messages
CREATE TABLE public.support_messages
(
    id                  UUID                     NOT NULL PRIMARY KEY,
    conversation_id     UUID                     NOT NULL,
    sender_type         VARCHAR(32)              NOT NULL,
    sender_user_id      UUID,
    client_message_id   UUID,
    body                VARCHAR(4000)            NOT NULL,
    normalized_body     VARCHAR(4000)            NOT NULL,
    delivery_status     VARCHAR(32)              NOT NULL,
    visible_to_customer BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_support_messages_conversation
        FOREIGN KEY (conversation_id)
            REFERENCES public.support_conversations (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_support_messages_sender_user
        FOREIGN KEY (sender_user_id)
            REFERENCES public.user_details (id)
            ON DELETE SET NULL,
    CONSTRAINT chk_support_messages_sender_type
        CHECK (sender_type IN ('CUSTOMER', 'OWNER', 'SYSTEM')),
    CONSTRAINT chk_support_messages_delivery_status
        CHECK (delivery_status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_support_messages_conversation_created_at
    ON public.support_messages (conversation_id, created_at);

CREATE UNIQUE INDEX uq_support_messages_client_message_id
    ON public.support_messages (conversation_id, client_message_id)
    WHERE client_message_id IS NOT NULL;
