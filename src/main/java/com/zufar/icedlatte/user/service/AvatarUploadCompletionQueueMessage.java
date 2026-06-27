package com.zufar.icedlatte.user.service;

public sealed interface AvatarUploadCompletionQueueMessage
        permits AvatarUploadCompletionQueueMessage.Ready, AvatarUploadCompletionQueueMessage.Failed {

    boolean ready();

    default AvatarUploadCompletionCommand completionCommand() {
        throw new UnsupportedOperationException("Avatar upload queue message is not a completion.");
    }

    default AvatarUploadFailureCommand failureCommand() {
        throw new UnsupportedOperationException("Avatar upload queue message is not a failure.");
    }

    static AvatarUploadCompletionQueueMessage ready(AvatarUploadCompletionCommand command) {
        return new Ready(command);
    }

    static AvatarUploadCompletionQueueMessage failed(AvatarUploadFailureCommand command) {
        return new Failed(command);
    }

    record Ready(AvatarUploadCompletionCommand completionCommand) implements AvatarUploadCompletionQueueMessage {

        @Override
        public boolean ready() {
            return true;
        }
    }

    record Failed(AvatarUploadFailureCommand failureCommand) implements AvatarUploadCompletionQueueMessage {

        @Override
        public boolean ready() {
            return false;
        }
    }
}
