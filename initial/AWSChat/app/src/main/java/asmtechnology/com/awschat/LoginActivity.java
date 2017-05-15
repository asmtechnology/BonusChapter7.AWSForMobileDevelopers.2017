package asmtechnology.com.awschat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;

import asmtechnology.com.awschat.controllers.CognitoIdentityPoolController;
import asmtechnology.com.awschat.controllers.CognitoUserPoolController;
import asmtechnology.com.awschat.interfaces.CognitoIdentityPoolControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.CognitoUserPoolControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.CognitoUserPoolControllerUserDetailsHandler;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private EditText mUsernameView;
    private EditText mPasswordView;

    private LoginButton mFacebookLoginButton;
    private CallbackManager mFacebookCallbackManager;

    private String mGoogleClientId = "332977463957-i3lf03m5e16ai1obc9tfru36vuu887ln.apps.googleusercontent.com";
    private GoogleSignInOptions mGoogleSignInOptions;
    private GoogleApiClient mGoogleApiClient;
    private SignInButton mGoogleSignInButton;
    private int GOOGLE_SIGNIN_RESULT_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);

        Button mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mSignupButton = (Button) findViewById(R.id.signup_button);
        mSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displaySignupActivity();
            }
        });

        CognitoIdentityPoolController  identityPoolController = CognitoIdentityPoolController.getInstance(this);
        identityPoolController.mCredentialsProvider.clear();
        identityPoolController.mCredentialsProvider.clearCredentials();

        configureLoginWithFacebook();
        configureGoogleSignIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GOOGLE_SIGNIN_RESULT_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleGoogleSignInResult(result);
        } else {
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void configureGoogleSignIn() {

        mGoogleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(mGoogleClientId)
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, mGoogleSignInOptions)
                .build();

        mGoogleSignInButton = (SignInButton) findViewById(R.id.google_sign_in_button);
        mGoogleSignInButton.setSize(SignInButton.SIZE_STANDARD);
        mGoogleSignInButton.setScopes(mGoogleSignInOptions.getScopeArray());

        mGoogleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, GOOGLE_SIGNIN_RESULT_CODE);
            }
        });
    }

    private void configureLoginWithFacebook() {

        final Context context = this;

        LoginManager.getInstance().logOut();

        mFacebookLoginButton = (LoginButton) findViewById(R.id.facebook_login_button);
        mFacebookLoginButton.setReadPermissions(Arrays.asList("public_profile", "email"));

        mFacebookCallbackManager = CallbackManager.Factory.create();

        mFacebookLoginButton.registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {

                final String authToken = loginResult.getAccessToken().getToken();

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {

                        try {
                            String username = object.getString("name");
                            String email = object.getString("email");

                            CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(context);
                            identityPoolController.getFederatedIdentityForFacebook(authToken, username, email, new CognitoIdentityPoolControllerGenericHandler() {
                                @Override
                                public void didSucceed() {
                                    displaySuccessMessage();
                                }

                                @Override
                                public void didFail(Exception exception) {
                                    displayErrorMessage(exception);
                                }
                            });

                        } catch (JSONException e) {
                            displayErrorMessage(e);
                        }
                    }
                });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email");
                request.setParameters(parameters);
                request.executeAsync();

            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {
                displayErrorMessage(error);
            }
        });
    }

    private void attemptLogin() {
        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

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

        final CognitoUserPoolController userPoolController = CognitoUserPoolController.getInstance(this);
        userPoolController.login(username, password, new CognitoUserPoolControllerGenericHandler() {
            @Override
            public void didSucceed() {
                getFederatedIdentity(userPoolController.getCurrentUser(), userPoolController.getUserSession());
            }

            @Override
            public void didFail(Exception exception) {
                displayErrorMessage(exception);
            }
        });
    }

    private void displaySignupActivity() {
        Intent intent = new Intent(this, SignupActivity.class);
        startActivity(intent);
    }

    private void displayHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    private void displaySuccessMessage() {

        final Context context = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Login succesful!");
                builder.setTitle("Success");
                builder.setCancelable(false);

                builder.setPositiveButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                displayHomeActivity();
                            }
                        });

                final AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    private void displayErrorMessage(final Exception exception) {

        final Context context = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(exception.getMessage());
                builder.setTitle("Error");
                builder.setCancelable(false);

                builder.setPositiveButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                final AlertDialog alert = builder.create();

                alert.show();
            }
        });
    }

    private void displayErrorMessage(final String title, final String message) {

        final Context context = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(message);
                builder.setTitle(title);
                builder.setCancelable(false);

                builder.setPositiveButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                final AlertDialog alert = builder.create();

                alert.show();
            }
        });
    }

    private void handleGoogleSignInResult(GoogleSignInResult result) {

        if (result.isSuccess() == false) {
            displayErrorMessage("Error", "Google Sign-In Failed.");
            return;
        }

        GoogleSignInAccount acct = result.getSignInAccount();
        String authToken = acct.getIdToken();
        String username = acct.getDisplayName();
        String email = acct.getEmail();

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(this);
        identityPoolController.getFederatedIdentityForGoogle(authToken, username, email, new CognitoIdentityPoolControllerGenericHandler() {
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



    @Override
    public void onConnectionFailed(final ConnectionResult connectionResult) {
        displayErrorMessage("Error", connectionResult.getErrorMessage());
    }
}
