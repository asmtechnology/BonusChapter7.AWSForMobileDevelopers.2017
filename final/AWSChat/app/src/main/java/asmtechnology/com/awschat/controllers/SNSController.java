package asmtechnology.com.awschat.controllers;

import android.content.Context;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.InvalidParameterException;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import asmtechnology.com.awschat.interfaces.SNSControllerGenericHandler;

public class SNSController {

    public String gcmDeviceToken = null;

    //TO DO: Insert your SNS platform application settings here
    private Regions snsRegion = Regions.US_EAST_1;
    private String platformApplicationARN = "your SNS platform application ARN";

    private Context mContext;

    private static SNSController instance = null;
    private SNSController() {}

    public static SNSController getInstance(Context context) {
        if(instance == null) {
            instance = new SNSController();
        }

        instance.mContext = context;
        return instance;
    }

    public void registerToken(final SNSControllerGenericHandler completion) {

        if (gcmDeviceToken == null) {
            Exception e = new Exception("Missing  GCM token.");
            completion.didFail(e);
            return;
        }

        Runnable runnable = new Runnable() {
            public void run() {

                CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
                AmazonSNSClient snsClient = new AmazonSNSClient(identityPoolController.mCredentialsProvider);
                snsClient.setRegion(Region.getRegion(snsRegion));

                String endpointArn = null;
                try {
                    endpointArn = createEndpoint(snsClient, gcmDeviceToken, platformApplicationARN);
                } catch (Exception e) {
                    completion.didFail(e);
                    return;
                }

                // update the GCM token associated with the platform endPoint, in case
                // the end point has an old token.
                System.out.println("Updating platform endpoint " + endpointArn);
                Map attribs = new HashMap();
                attribs.put("Token", gcmDeviceToken);
                attribs.put("Enabled", "true");
                SetEndpointAttributesRequest saeReq =  new SetEndpointAttributesRequest()
                        .withEndpointArn(endpointArn)
                        .withAttributes(attribs);
                snsClient.setEndpointAttributes(saeReq);

                completion.didSucceed();
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    private String createEndpoint(AmazonSNSClient client, String token, String applicationArn) throws InvalidParameterException {

        String endpointArn = null;
        try {

            CreatePlatformEndpointRequest cpeReq =  new CreatePlatformEndpointRequest()
                    .withPlatformApplicationArn(applicationArn)
                    .withToken(token);
            CreatePlatformEndpointResult cpeRes = client.createPlatformEndpoint(cpeReq);
            endpointArn = cpeRes.getEndpointArn();

        } catch (InvalidParameterException e) {

            String message = e.getErrorMessage();
            System.out.println("Exception message: " + message);
            Pattern p = Pattern
                    .compile(".*Endpoint (arn:aws:sns[^ ]+) already exists " +
                            "with the same token.*");
            Matcher m = p.matcher(message);
            if (m.matches()) {
                endpointArn = m.group(1);
            } else {
                throw e;
            }
        }

        return endpointArn;
    }
}

