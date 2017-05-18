package asmtechnology.com.awschat.interfaces;

public interface SNSControllerGenericHandler {
    void didSucceed();
    void didFail(Exception exception);
}
