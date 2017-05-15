package asmtechnology.com.awschat.interfaces;

import java.util.ArrayList;

public interface DynamoDBControllerRetrieveFriendIDsHandler {
    void didSucceed(ArrayList<String> results);
    void didFail(Exception exception);
}
