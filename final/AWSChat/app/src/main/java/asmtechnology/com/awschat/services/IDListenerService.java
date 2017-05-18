package asmtechnology.com.awschat.services;


import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;

public class IDListenerService extends InstanceIDListenerService {
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }
}