package asmtechnology.com.awschat.interfaces;

public interface S3ControllerGenericHandler {
    void didSucceed();
    void didFail(Exception exception);
}
