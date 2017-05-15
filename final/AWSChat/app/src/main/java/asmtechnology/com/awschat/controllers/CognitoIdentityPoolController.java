package asmtechnology.com.awschat.controllers;

import android.content.Context;
import android.os.AsyncTask;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.amazonaws.regions.Regions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmtechnology.com.awschat.interfaces.CognitoIdentityPoolControllerGenericHandler;


public class CognitoIdentityPoolController {

    private String identityPoolID = "us-east-1:3a7a90d7-b0e7-4730-b0aa-866c9a16ec85";
    private Regions identityPoolRegion = Regions.US_EAST_1;

    public CognitoCachingCredentialsProvider mCredentialsProvider;

    private Context mContext;
    private CognitoIdentityPoolControllerGenericHandler facebookCompletionHandler;
    private CognitoIdentityPoolControllerGenericHandler googleCompletionHandler;
    private CognitoIdentityPoolControllerGenericHandler amazonCompletionHandler;

    private static CognitoIdentityPoolController instance = null;
    private CognitoIdentityPoolController() {}

    public static CognitoIdentityPoolController getInstance(Context context) {
        if(instance == null) {
            instance = new CognitoIdentityPoolController();
        }

        if (context != null) {
            instance.setupCredentialsProvider(context);
        }

        return instance;
    }

    private void  setupCredentialsProvider(Context context) {

        if (mCredentialsProvider == null) {
            mContext = context;
            mCredentialsProvider = new CognitoCachingCredentialsProvider(mContext, identityPoolID, identityPoolRegion);
            return;
        }

        if (mContext != context) {
            mCredentialsProvider = new CognitoCachingCredentialsProvider(mContext, identityPoolID, identityPoolRegion);
        }
    }

    public void getFederatedIdentityForFacebook(String idToken,
                                                String username,
                                                String emailAddress,
                                                final CognitoIdentityPoolControllerGenericHandler completion) {

        this.facebookCompletionHandler = completion;
        new FacebookIdentityFederationTask().execute(idToken, username, emailAddress);
    }


    public void getFederatedIdentityForGoogle(String idToken,
                                                String username,
                                                String emailAddress,
                                                final CognitoIdentityPoolControllerGenericHandler completion) {

        this.googleCompletionHandler = completion;
        new GoogleIdentityFederationTask().execute(idToken, username, emailAddress);
    }

    public void getFederatedIdentityForAmazon(String idToken,
                                              String username,
                                              String emailAddress,
                                              String userPoolRegion,
                                              String userPoolID,
                                              final CognitoIdentityPoolControllerGenericHandler completion) {

        this.amazonCompletionHandler = completion;
        new AmazonIdentityFederationTask().execute(idToken, username, emailAddress, userPoolRegion, userPoolID);
    }




    class FacebookIdentityFederationTask extends AsyncTask<String, Void, Long> {

        private String idToken;
        private String username;
        private String emailAddress;

        protected Long doInBackground(String... strings) {

            idToken = strings[0];
            username = strings[1];
            emailAddress = strings[2];

            Map<String, String> logins = new HashMap<String, String>();
            logins.put("graph.facebook.com", idToken);
            mCredentialsProvider.setLogins(logins);
            mCredentialsProvider.refresh();

            return 1L;
        }

        protected void onPostExecute(Long result) {

            CognitoSyncManager client = new CognitoSyncManager(mContext,  identityPoolRegion,  mCredentialsProvider);

            Dataset dataset = client.openOrCreateDataset("facebookUserData");
            dataset.put("name", username);
            dataset.put("email", emailAddress);

            dataset.synchronize(new Dataset.SyncCallback() {
                @Override
                public void onSuccess(Dataset dataset, List<Record> updatedRecords) {
                    facebookCompletionHandler.didSucceed();
                }

                @Override
                public boolean onConflict(Dataset dataset, List<SyncConflict> conflicts) {
                    List<Record> resolved = new ArrayList<Record>();
                    for (SyncConflict conflict : conflicts) {
                        resolved.add(conflict.resolveWithRemoteRecord());
                    }
                    dataset.resolve(resolved);
                    return true;
                }

                @Override
                public boolean onDatasetDeleted(Dataset dataset, String datasetName) {
                    return true;
                }

                @Override
                public boolean onDatasetsMerged(Dataset dataset, List<String> datasetNames) {
                    return false;
                }

                @Override
                public void onFailure(DataStorageException dse) {
                    facebookCompletionHandler.didFail(dse);
                }
            });

        }
    }



    class GoogleIdentityFederationTask extends AsyncTask<String, Void, Long> {

        private String idToken;
        private String username;
        private String emailAddress;

        protected Long doInBackground(String... strings) {

            idToken = strings[0];
            username = strings[1];
            emailAddress = strings[2];

            Map<String, String> logins = new HashMap<String, String>();
            logins.put("accounts.google.com", idToken);

            mCredentialsProvider.clearCredentials();
            mCredentialsProvider.clear();

            mCredentialsProvider.setLogins(logins);
            mCredentialsProvider.refresh();

            return 1L;
        }

        protected void onPostExecute(Long result) {

            CognitoSyncManager client = new CognitoSyncManager(mContext,  identityPoolRegion,  mCredentialsProvider);

            Dataset dataset = client.openOrCreateDataset("googleUserData");
            dataset.put("name", username);
            dataset.put("email", emailAddress);

            dataset.synchronize(new Dataset.SyncCallback() {
                @Override
                public void onSuccess(Dataset dataset, List<Record> updatedRecords) {
                    googleCompletionHandler.didSucceed();
                }

                @Override
                public boolean onConflict(Dataset dataset, List<SyncConflict> conflicts) {
                    List<Record> resolved = new ArrayList<Record>();
                    for (SyncConflict conflict : conflicts) {
                        resolved.add(conflict.resolveWithRemoteRecord());
                    }
                    dataset.resolve(resolved);
                    return true;
                }

                @Override
                public boolean onDatasetDeleted(Dataset dataset, String datasetName) {
                    return true;
                }

                @Override
                public boolean onDatasetsMerged(Dataset dataset, List<String> datasetNames) {
                    return false;
                }

                @Override
                public void onFailure(DataStorageException dse) {
                    googleCompletionHandler.didFail(dse);
                }
            });

        }
    }



    class AmazonIdentityFederationTask extends AsyncTask<String, Void, Long> {

        private String idToken;
        private String username;
        private String emailAddress;
        private String userPoolRegion;
        private String userPoolID;

        protected Long doInBackground(String... strings) {

            idToken = strings[0];
            username = strings[1];
            emailAddress = strings[2];
            userPoolRegion = strings[3];
            userPoolID = strings[4];

            String key = "cognito-idp." + userPoolRegion + ".amazonaws.com/" + userPoolID;
            Map<String, String> logins = new HashMap<String, String>();
            logins.put(key, idToken);

            mCredentialsProvider.clearCredentials();
            mCredentialsProvider.clear();

            mCredentialsProvider.setLogins(logins);
            mCredentialsProvider.refresh();

            return 1L;
        }

        protected void onPostExecute(Long result) {

            CognitoSyncManager client = new CognitoSyncManager(mContext,  identityPoolRegion,  mCredentialsProvider);

            Dataset dataset = client.openOrCreateDataset("amazonUserData");
            dataset.put("name", username);
            dataset.put("email", emailAddress);

            dataset.synchronize(new Dataset.SyncCallback() {
                @Override
                public void onSuccess(Dataset dataset, List<Record> updatedRecords) {
                    amazonCompletionHandler.didSucceed();
                }

                @Override
                public boolean onConflict(Dataset dataset, List<SyncConflict> conflicts) {
                    List<Record> resolved = new ArrayList<Record>();
                    for (SyncConflict conflict : conflicts) {
                        resolved.add(conflict.resolveWithRemoteRecord());
                    }
                    dataset.resolve(resolved);
                    return true;
                }

                @Override
                public boolean onDatasetDeleted(Dataset dataset, String datasetName) {
                    return true;
                }

                @Override
                public boolean onDatasetsMerged(Dataset dataset, List<String> datasetNames) {
                    return false;
                }

                @Override
                public void onFailure(DataStorageException dse) {
                    amazonCompletionHandler.didFail(dse);
                }
            });

        }
    }
}
