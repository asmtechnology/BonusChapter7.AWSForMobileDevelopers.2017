package asmtechnology.com.awschat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;

import java.util.Map;

import asmtechnology.com.awschat.controllers.CognitoIdentityPoolController;
import asmtechnology.com.awschat.controllers.CognitoUserPoolController;
import asmtechnology.com.awschat.interfaces.CognitoIdentityPoolControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.CognitoUserPoolControllerConfirmSignupHandler;
import asmtechnology.com.awschat.interfaces.CognitoUserPoolControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.CognitoUserPoolControllerSignupHandler;
import asmtechnology.com.awschat.interfaces.CognitoUserPoolControllerUserDetailsHandler;

public class SignupActivity extends AppCompatActivity {

    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mEmailView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mEmailView = (EditText) findViewById(R.id.emailAddress);

        Button mCreateAccountButton = (Button) findViewById(R.id.create_account_button);
        mCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSignup();
            }
        });
    }

    private void doSignup() {

        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mEmailView.setError(null);

        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String email = mEmailView.getText().toString();

        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            mUsernameView.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            mPasswordView.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            mEmailView.requestFocus();
            return;
        }

        CognitoUserPoolController userPoolController = CognitoUserPoolController.getInstance(this);
        userPoolController.signup(username, password, email, new CognitoUserPoolControllerSignupHandler() {
            @Override
            public void didSucceed(CognitoUser user, CognitoUserSession session, boolean userMustConfirmEmailAddress) {
                if (userMustConfirmEmailAddress) {
                    requestConfirmationCode(user);
                } else {
                    getFederatedIdentity(user, session);
                }
            }

            @Override
            public void didFail(Exception exception) {
                displayErrorMessage(exception);
            }
        });
    }


    private void displayHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    private void displaySuccessMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your account has been created!.");
        builder.setTitle("Success");
        builder.setCancelable(false);

        builder.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        displayHomeActivity();
                    }
                });

        final AlertDialog alert = builder.create();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alert.show();
            }
        });
    }

    private void displayErrorMessage(Exception exception) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(exception.getMessage());
        builder.setTitle("Error");
        builder.setCancelable(false);

        builder.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        final AlertDialog alert = builder.create();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alert.show();
            }
        });

    }

    private void requestConfirmationCode(final CognitoUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        View customInflatedView = inflater.inflate(R.layout.dialog_confirm_signup, null);
        final EditText confirmationCode = (EditText) customInflatedView.findViewById(R.id.confirmationCode);
        builder.setView(customInflatedView);

        builder.setPositiveButton("Ok",  new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, int id) {

                String codeEntered = confirmationCode.getText().toString();

                if (codeEntered.length() == 0) {
                    dialog.dismiss();
                    requestConfirmationCode(user);
                    return;
                }

                dialog.dismiss();
                verifyConfirmationCode(user, codeEntered);
            }
        });

        builder.setNeutralButton("Resend code", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                resendConfirmationCode(user);
            }
        });

        final AlertDialog alert = builder.create();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alert.show();
            }
        });
    }

    private void verifyConfirmationCode(final CognitoUser user, String code) {

        String password = mPasswordView.getText().toString();

        CognitoUserPoolController userPoolController = CognitoUserPoolController.getInstance(this);
        userPoolController.confirmSignup(user, password, code, new CognitoUserPoolControllerConfirmSignupHandler() {
            @Override
            public void didSucceed(CognitoUser user, CognitoUserSession session) {
                getFederatedIdentity(user,session);
            }

            @Override
            public void didFail(Exception exception) {
                requestConfirmationCode(user);
            }
        });
    }

    private void resendConfirmationCode(final CognitoUser user) {

        CognitoUserPoolController userPoolController = CognitoUserPoolController.getInstance(this);

        userPoolController.resendConfirmationCode(user, new CognitoUserPoolControllerGenericHandler() {
            @Override
            public void didSucceed() {
                displayCodeResentMessage(user);
            }

            @Override
            public void didFail(Exception exception) {
                displayErrorMessage(exception);
            }
        });
    }

    private void displayCodeResentMessage(final CognitoUser user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("A 6-digit confirmation code has been sent to your email address.");
        builder.setTitle("Code Resent.");
        builder.setCancelable(false);

        builder.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        requestConfirmationCode(user);
                    }
                });

        final AlertDialog alert = builder.create();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alert.show();
            }
        });

    }

    private void getFederatedIdentity(final CognitoUser cognitoUser, final CognitoUserSession userSession) {

        final Context context = this;
        final CognitoUserPoolController userPoolController = CognitoUserPoolController.getInstance(this);

        userPoolController.getUserDetails(cognitoUser, new CognitoUserPoolControllerUserDetailsHandler() {

            @Override
            public void didSucceed(CognitoUserDetails userDetails) {

                CognitoUserAttributes userAttributes = userDetails.getAttributes();
                Map attributeMap    = userAttributes.getAttributes();

                String authToken = userSession.getIdToken().getJWTToken();
                String username = mUsernameView.getText().toString();
                String email = attributeMap.get("email").toString();

                CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(context);
                identityPoolController.getFederatedIdentityForAmazon(authToken,  username, email,
                        userPoolController.getUserPoolRegion(),
                        userPoolController.getUserPoolID(),
                        new CognitoIdentityPoolControllerGenericHandler() {
                            @Override
                            public void didSucceed() {
                                displaySuccessMessage();
                            }

                            @Override
                            public void didFail(Exception exception) {
                                displayErrorMessage(exception);
                            }
                        });

            }

            @Override
            public void didFail(Exception exception) {
                displayErrorMessage(exception);
            }
        });
    }
}
