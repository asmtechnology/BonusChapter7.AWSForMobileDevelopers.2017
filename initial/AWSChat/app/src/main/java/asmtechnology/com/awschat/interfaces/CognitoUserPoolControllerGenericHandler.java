package asmtechnology.com.awschat.interfaces;

public interface CognitoUserPoolControllerGenericHandler {
    void didSucceed();
    void didFail(Exception exception);
}
