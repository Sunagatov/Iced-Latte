package com.zufar.icedlatte.supportchat.realtime;

import com.zufar.icedlatte.supportchat.entity.SupportConversationEntity;
import com.zufar.icedlatte.supportchat.entity.SupportMessageEntity;

public interface SupportChatMessagePublisher {

    void publishOwnerReply(SupportConversationEntity conversation, SupportMessageEntity message);
}
