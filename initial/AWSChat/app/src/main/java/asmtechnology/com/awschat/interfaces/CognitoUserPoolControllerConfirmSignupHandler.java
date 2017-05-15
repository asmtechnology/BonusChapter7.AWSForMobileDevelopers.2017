package asmtechnology.com.awschat.interfaces;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;

public interface CognitoUserPoolControllerConfirmSignupHandler {
    void didSucceed(CognitoUser user, CognitoUserSession session);
    void didFail(Exception exception);
}
