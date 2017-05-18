package asmtechnology.com.awschat.services;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import asmtechnology.com.awschat.R;

public class GCMListenerService extends GcmListenerService {
    @Override
    public void onMessageReceived(String from, Bundle data) {

        String message = data.getString("message");

        Log.d("AWSChat", "From: " + from);
        Log.d("AWSChat", "Message: " + message);

        NotificationManager notificationManager= (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("AWSChat")
                .setContentText(message)
                .setAutoCancel(true);
        notificationManager.notify(1, mBuilder.build());
    }

}
