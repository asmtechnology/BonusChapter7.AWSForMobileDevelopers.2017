package asmtechnology.com.awschat.interfaces;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;

public interface CognitoUserPoolControllerUserDetailsHandler {
    void didSucceed(CognitoUserDetails userDetails);
    void didFail(Exception exception);
}