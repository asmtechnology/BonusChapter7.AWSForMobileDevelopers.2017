package asmtechnology.com.awschat.interfaces;

public interface DynamoDBControllerGenericHandler {
    void didSucceed();
    void didFail(Exception exception);
}
