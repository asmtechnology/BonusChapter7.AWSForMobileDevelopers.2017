package asmtechnology.com.awschat;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import asmtechnology.com.awschat.controllers.ChatManager;
import asmtechnology.com.awschat.controllers.CognitoIdentityPoolController;
import asmtechnology.com.awschat.controllers.DynamoDBController;
import asmtechnology.com.awschat.interfaces.DynamoDBControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.RecyclerViewHolderListener;
import asmtechnology.com.awschat.models.User;
import asmtechnology.com.awschat.recyclerview.FriendListAdapter;

public class HomeActivity extends AppCompatActivity implements RecyclerViewHolderListener {

    private RecyclerView mRecyclerView;
    private FriendListAdapter mAdapter;

    private int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 382;
    private boolean mWriteExternalStoragePermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        this.setTitle("Friend List");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mAdapter = new FriendListAdapter(this, this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        refreshFriendList();
        checkPermissions();
    }

    protected void onResume() {
        super.onResume();
        refreshFriendList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.add_friend) {
            Intent intent = new Intent(this, AddFriendActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshFriendList() {

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(this);
        if (identityPoolController.mCredentialsProvider == null) {
            displayMessage("Error", "Cognito Identity has expired. User must login again");
            return;
        }

        String userId = identityPoolController.mCredentialsProvider.getIdentityId();
        if ((userId == null) || (userId.length() == 0)) {
            displayMessage("Error", "Cognito Identity has expired. User must login again");
            return;
        }

        DynamoDBController dynamoDBController = DynamoDBController.getInstance(this);
        dynamoDBController.refreshFriendList(userId, new DynamoDBControllerGenericHandler() {
            @Override
            public void didSucceed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void didFail(Exception exception) {
                displayMessage("Error", exception.getMessage());
            }
        });
    }

    private void displayMessage(final String title, final String message) {

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

    public void didTapOnRowAtIndex(int selectedIndex) {

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(this);
        if (identityPoolController.mCredentialsProvider == null) {
            displayMessage("Error", "Cognito Identity has expired. User must login again");
            return;
        }

        String fromUserId = identityPoolController.mCredentialsProvider.getIdentityId();
        if ((fromUserId == null) || (fromUserId.length() == 0)) {
            displayMessage("Error", "Cognito Identity has expired. User must login again");
            return;
        }

        ChatManager chatManager = ChatManager.getInstance(this);
        User otherUser = chatManager.friendList.get(selectedIndex);
        String toUserId = otherUser.getId();

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("FROM_USER_ID", fromUserId);
        intent.putExtra("TO_USER_ID", toUserId);
        startActivity(intent);

    }


    private void checkPermissions() {

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE);

        } else {
            mWriteExternalStoragePermissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mWriteExternalStoragePermissionGranted = true;
            } else {
                mWriteExternalStoragePermissionGranted = false;
            }
        }

    }
}
