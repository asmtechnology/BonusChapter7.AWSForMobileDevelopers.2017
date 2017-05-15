package asmtechnology.com.awschat;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import asmtechnology.com.awschat.controllers.ChatManager;
import asmtechnology.com.awschat.interfaces.ChatManagerGenericHandler;
import asmtechnology.com.awschat.interfaces.ChatManagerLoadChatHandler;
import asmtechnology.com.awschat.models.Chat;

public class UploadImageActivity extends AppCompatActivity {

    private Chat currentChat = null;
    private String fromUserId;
    private String toUserId;

    private String mSelectedImagePath = null;

    private Button mSelectImageButton;
    private Button mUploadImageButton;
    private ImageView mImageView;
    private ProgressDialog mDialog = null;

    private int REQUESTCODE_IMAGEPICKER = 998;
    private int REQUEST_PERMISSIONS = 999;

    private boolean mReadExternalStoragePermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        this.setTitle("Select Image");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fromUserId = getIntent().getStringExtra("FROM_USER_ID");
        toUserId = getIntent().getStringExtra("TO_USER_ID");

        mSelectImageButton = (Button) findViewById(R.id.select_image_button);
        mUploadImageButton = (Button) findViewById(R.id.upload_image_button);
        mImageView = (ImageView) findViewById(R.id.imageView);

        setupSelectImageButton();
        setupUploadImageButton();

        checkPermissions();
    }

    private void setupSelectImageButton() {
        mSelectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mReadExternalStoragePermissionGranted == false) {
                    displayPermissionsError();
                    return;
                }

                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent , REQUESTCODE_IMAGEPICKER);

            }
        });
    }

    private void setupUploadImageButton() {

        final Context context = this;

        mUploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSelectedImagePath == null) {
                    return;
                }

                sendImage(mSelectedImagePath);

            }
        });
    }

    private void checkPermissions() {

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);

        } else {
            mReadExternalStoragePermissionGranted = true;
        }
    }

    private void sendImage(String filePath) {

        disableUI();
        showProgressDialog();


        String pngFilePath = filePath;
        String extension = FilenameUtils.getExtension(filePath).toLowerCase();
        if (extension != "png") {
            pngFilePath = convertToPNG(filePath);
        }

        if (pngFilePath == null) {
            enableUI();
            hideProgressDialog();
            displayFormatConversionError();
            return;
        }

        final String inputImagePath = pngFilePath;
        final boolean shouldDeleteInputFileAfterUpload = (extension != "png") ? true : false;
        final ChatManager chatManager = ChatManager.getInstance(this);
        chatManager.loadChat(fromUserId, toUserId, new ChatManagerLoadChatHandler() {

            @Override
            public void didSucceed(Chat chat) {
                chatManager.sendImage(chat, inputImagePath, new ChatManagerGenericHandler() {
                    @Override
                    public void didSucceed() {

                        File f = new File(inputImagePath);
                        if (f.exists() && shouldDeleteInputFileAfterUpload == true) {
                            f.delete();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enableUI();
                                hideProgressDialog();
                                finish();
                            }
                        });

                    }

                    @Override
                    public void didFail(final Exception exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enableUI();
                                hideProgressDialog();
                                displayErrorMessage(exception);
                            }
                        });
                    }
                });
            }

            @Override
            public void didFail(final Exception exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableUI();
                        hideProgressDialog();
                        displayErrorMessage(exception);
                    }
                });
            }
        });
    }


    @SuppressLint("NewApi")
    private String getPath(Uri uri) {

        final boolean needToCheckUri = Build.VERSION.SDK_INT >= 19;
        String selection = null;
        String[] selectionArgs = null;

        if (needToCheckUri && DocumentsContract.isDocumentUri(getApplicationContext(), uri)) {

            if (isExternalStorageDocument(uri)) {

                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];

            } else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

            } else if (isMediaDocument(uri)) {

                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[] {split[1]};
            }
        }

        if ("content".equalsIgnoreCase(uri.getScheme())) {

            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = null;

            try {
                cursor = getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);

                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }

            } catch (Exception e) {
                Log.e("AWSCHAT", "Unable to resolve uri into filepath", e);
                return null;
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }


    private void disableUI() {
        mUploadImageButton.setEnabled(false);
        mSelectImageButton.setEnabled(false);
    }

    private void enableUI() {
        mUploadImageButton.setEnabled(true);
        mSelectImageButton.setEnabled(true);
    }

    private void showProgressDialog() {
        if (mDialog == null) {
            mDialog = new ProgressDialog(this);
            mDialog.setMessage("Loading...");
            mDialog.setCancelable(false);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        mDialog.show();
    }

    private void hideProgressDialog() {
        if (mDialog != null) {
            mDialog.hide();
        }
        mDialog = null;
    }

    private void displayPermissionsError() {
        final Context context = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("You have not granted this app permission to access media files on this device.");
                builder.setTitle("Permission Error");
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


    private void displayFormatConversionError() {
        final Context context = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Unable to convert the selected image to a PNG file");
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

    private String convertToPNG(String inputFilePath) {
        String folderPath = Environment.getExternalStorageDirectory().toString();
        String fileName = generateUUID() + ".png";
        String tempFilePath = folderPath + "/" + fileName;

        Bitmap b = BitmapFactory.decodeFile(inputFilePath);
        if (b == null) {
            return null;
        }

        try {
            FileOutputStream out = new FileOutputStream(tempFilePath);
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            Log.e("AWSChat", "Failed to create PNG file");
            return null;
        }

        return tempFilePath;
    }

    private String generateUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        return uuidString.toUpperCase();
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUESTCODE_IMAGEPICKER) {

            if (resultCode == RESULT_OK) {
                Uri selectedImageURI = intent.getData();
                mSelectedImagePath = getPath(selectedImageURI);

                Bitmap b = BitmapFactory.decodeFile(mSelectedImagePath);
                mImageView.setImageBitmap(b);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mReadExternalStoragePermissionGranted = true;
            } else {
                mReadExternalStoragePermissionGranted = false;
            }
        }

    }
}
