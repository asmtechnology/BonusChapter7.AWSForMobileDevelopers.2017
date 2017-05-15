package asmtechnology.com.awschat;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import asmtechnology.com.awschat.controllers.ChatManager;
import asmtechnology.com.awschat.controllers.CognitoIdentityPoolController;
import asmtechnology.com.awschat.controllers.DynamoDBController;
import asmtechnology.com.awschat.interfaces.DynamoDBControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.RecyclerViewHolderListener;
import asmtechnology.com.awschat.models.User;
import asmtechnology.com.awschat.recyclerview.PotentialFriendListAdapter;

public class AddFriendActivity extends AppCompatActivity implements RecyclerViewHolderListener {

    private RecyclerView mRecyclerView;
    private PotentialFriendListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        this.setTitle("Add Friend");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mAdapter = new PotentialFriendListAdapter(this, this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        refreshPotentialFriendList();
    }

    private void refreshPotentialFriendList() {

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
        dynamoDBController.refreshPotentialFriendList(userId, new DynamoDBControllerGenericHandler() {
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

        // add selected user as a friend.
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

        ChatManager chatManager = ChatManager.getInstance(this);
        User potentialFriend = chatManager.potentialFriendList.get(selectedIndex);

        DynamoDBController dynamoDBController = DynamoDBController.getInstance(this);
        dynamoDBController.addFriend(userId, potentialFriend.getId(), new DynamoDBControllerGenericHandler() {

            @Override
            public void didSucceed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }

            @Override
            public void didFail(Exception exception) {
                displayMessage("Error", exception.getMessage());
            }
        });



    }
}
