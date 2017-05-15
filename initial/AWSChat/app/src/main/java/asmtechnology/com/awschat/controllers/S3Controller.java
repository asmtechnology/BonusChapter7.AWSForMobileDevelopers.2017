package asmtechnology.com.awschat.controllers;


import android.content.Context;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

import asmtechnology.com.awschat.interfaces.S3ControllerGenericHandler;

public class S3Controller {

    private Regions bucketRegion = Regions.US_EAST_1;
    private String imageBucketName = "com.asmtechnology.awschat.images";
    private String thumbnailsBucketName = "com.asmtechnology.awschat.thumbnails";

    private Context mContext;

    private static S3Controller instance = null;
    private S3Controller() {}

    public static S3Controller getInstance(Context context) {
        if(instance == null) {
            instance = new S3Controller();
        }

        instance.mContext = context;
        return instance;
    }

    public void uploadImage(String localFilePath, String remoteFileName, String remoteFileExtension, final S3ControllerGenericHandler completion) {

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
        AmazonS3Client sS3Client = new AmazonS3Client(identityPoolController.mCredentialsProvider);
        sS3Client.setRegion(Region.getRegion(bucketRegion));

        final TransferUtility transferUtility = new TransferUtility(sS3Client, mContext);

        final File file = new File(localFilePath);
        final String imageKey = remoteFileName + "." + remoteFileExtension;

        Runnable runnable = new Runnable() {
            public void run() {

                TransferObserver observer = transferUtility.upload(imageBucketName, imageKey, file);
                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        Log.d("AWSChat", "onStateChanged: " + id + ", " + state);
                        if (state == TransferState.COMPLETED){
                            completion.didSucceed();
                            return;
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        int percentage = (int) (bytesCurrent/bytesTotal * 100);
                        String message = "Uploaded " + Integer.toString(percentage) + "% to file" + imageKey;
                        Log.d("AWSChat", message);
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        completion.didFail(ex);
                    }
                });

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void downloadThumbnail(final String localFilePath, final String remoteFileName, final S3ControllerGenericHandler completion) {

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
        AmazonS3Client sS3Client = new AmazonS3Client(identityPoolController.mCredentialsProvider);
        sS3Client.setRegion(Region.getRegion(bucketRegion));

        final TransferUtility transferUtility = new TransferUtility(sS3Client, mContext);

        // delete existing file (if it exists)

        final File file = new File(localFilePath);
        if (file.exists()) {
            file.delete();
        }

        Runnable runnable = new Runnable() {
            public void run() {

                String s3Key = remoteFileName + ".png";
                TransferObserver observer = transferUtility.download(thumbnailsBucketName, s3Key, file);
                observer.setTransferListener(new TransferListener() {
                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state == TransferState.COMPLETED){
                            completion.didSucceed();
                            return;
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal != 0) {
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                            String message = "Downloaded " + Integer.toString(percentage) + "% to file" + localFilePath;
                            Log.d("AWSChat", message);
                        }
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        completion.didFail(ex);
                    }
                });

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }


}
