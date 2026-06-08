package com.zufar.icedlatte.supportchat.telegram;

import java.util.Optional;

interface TelegramBotClient {

    Optional<TelegramForumTopic> createForumTopic(String name);

    Optional<TelegramMessageRef> sendMessage(Long messageThreadId, String text);
}
