package asmtechnology.com.awschat.interfaces;

public interface DynamoDBControllerRetrieveChatHandler {
    void didSucceed();
    void didNotFindChat();
    void didFail(Exception exception);
}
