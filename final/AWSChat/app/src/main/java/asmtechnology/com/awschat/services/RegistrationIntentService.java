package asmtechnology.com.awschat.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import asmtechnology.com.awschat.R;
import asmtechnology.com.awschat.controllers.SNSController;

public class RegistrationIntentService extends IntentService {

    public RegistrationIntentService() {
        super("AWSChat");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            sendRegistrationToServer(token);
        } catch (Exception e) {
            Log.d("AWSChat", "Failed to register with GCM.", e);
        }
    }

    private void sendRegistrationToServer(String token) {
        SNSController snsController = SNSController.getInstance(this);
        snsController.gcmDeviceToken = token;
    }

}