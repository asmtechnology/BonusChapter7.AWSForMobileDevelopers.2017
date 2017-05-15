package asmtechnology.com.awschat.interfaces;

import asmtechnology.com.awschat.models.User;

public interface DynamoDBControllerRetrieveUserHandler {
    void didSucceed(User user);
    void didFail(Exception exception);
}
