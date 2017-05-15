package asmtechnology.com.awschat.interfaces;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;

public interface CognitoUserPoolControllerSignupHandler {
    void didSucceed(CognitoUser user, CognitoUserSession session, boolean userMustConfirmEmailAddress);
    void didFail(Exception exception);
}
