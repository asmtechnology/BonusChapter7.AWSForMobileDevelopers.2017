package asmtechnology.com.awschat.interfaces;

public interface CognitoIdentityPoolControllerGenericHandler {
    void didSucceed();
    void didFail(Exception exception);
}
