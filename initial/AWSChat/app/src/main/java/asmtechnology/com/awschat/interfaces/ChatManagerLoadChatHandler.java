package asmtechnology.com.awschat.interfaces;

import asmtechnology.com.awschat.models.Chat;

public interface ChatManagerLoadChatHandler {
    void didSucceed(Chat chat);
    void didFail(Exception exception);
}

