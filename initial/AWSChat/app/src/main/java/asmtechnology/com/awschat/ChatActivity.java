package asmtechnology.com.awschat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import asmtechnology.com.awschat.controllers.ChatManager;
import asmtechnology.com.awschat.interfaces.ChatManagerGenericHandler;
import asmtechnology.com.awschat.interfaces.ChatManagerLoadChatHandler;
import asmtechnology.com.awschat.interfaces.RecyclerViewHolderListener;
import asmtechnology.com.awschat.models.Chat;
import asmtechnology.com.awschat.recyclerview.ChatListAdapter;


public class ChatActivity extends AppCompatActivity implements RecyclerViewHolderListener {

    private Chat currentChat = null;
    private String fromUserId;
    private String toUserId;
    private ChatListAdapter mAdapter;

    private CoordinatorLayout mCoordinatorLayout;
    private LinearLayout mLayout1;
    private RecyclerView mRecyclerView;
    private EditText mMessageTextView;
    private Button mUploadImageButton;
    private Button mSendTextButton;

    private boolean mKeyboardIsVisible = false;
    private int mKeyboardHeight = 0;
    private ProgressDialog mDialog = null;

    private int REQUESTCODE_UPLOAD_ACTIVITY = 381;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fromUserId = getIntent().getStringExtra("FROM_USER_ID");
        toUserId = getIntent().getStringExtra("TO_USER_ID");

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.layout_0);
        mLayout1 = (LinearLayout) findViewById(R.id.layout_1);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mMessageTextView = (EditText) findViewById(R.id.message_text);
        mUploadImageButton = (Button) findViewById(R.id.upload_image_button);
        mSendTextButton = (Button) findViewById(R.id.send_text_button);

        setupRecyclerView();
        setupUploadImageButton();
        setupSendTextButton();
        setupRelativeLayout();
        setupGlobalLayoutListener();

        disableUI();
        showProgressDialog();

        if (currentChat == null) {
            prepareForChat(fromUserId, toUserId);
        } else {
            refreshMessages();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.refresh_chat) {
            disableUI();
            showProgressDialog();
            refreshMessages();
        }

        return super.onOptionsItemSelected(item);
    }


    private void prepareForChat(String fromUserId, String toUserId) {

        final ChatManager chatManager = ChatManager.getInstance(this);
        chatManager.loadChat(fromUserId, toUserId, new ChatManagerLoadChatHandler() {

            @Override
            public void didSucceed(Chat chat) {
                currentChat = chat;
                mAdapter.setChat(chat);

                chatManager.refreshAllMessages(chat, new ChatManagerGenericHandler() {
                    @Override
                    public void didSucceed() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enableUI();
                                hideProgressDialog();
                                mAdapter.notifyDataSetChanged();

                                mRecyclerView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                                    }
                                }, 500);
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

    private void refreshMessages() {

        final ChatManager chatManager = ChatManager.getInstance(this);
        chatManager.refreshAllMessages(currentChat, new ChatManagerGenericHandler() {
            @Override
            public void didSucceed() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableUI();
                        hideProgressDialog();
                        mAdapter.notifyDataSetChanged();

                        mRecyclerView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                            }
                        }, 500);
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

    private void setupRecyclerView() {
        mAdapter = new ChatListAdapter(this, this, null, fromUserId, ChatActivity.this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
    }

    private void setupUploadImageButton() {
        final Context context = this;

        mUploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, UploadImageActivity.class);
                intent.putExtra("FROM_USER_ID", fromUserId);
                intent.putExtra("TO_USER_ID", toUserId);
                startActivityForResult(intent, REQUESTCODE_UPLOAD_ACTIVITY);
            }
        });
    }

    private void setupSendTextButton() {

        final Context context = this;

        mSendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String textToSend = mMessageTextView.getText().toString();
                if (TextUtils.isEmpty(textToSend)) {
                    return;
                }

                if (currentChat == null) {
                    return;
                }

                disableUI();

                ChatManager chatManager = ChatManager.getInstance(context);
                chatManager.sendTextMessage(currentChat, textToSend, new ChatManagerGenericHandler() {

                    @Override
                    public void didSucceed() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enableUI();
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                    }

                    @Override
                    public void didFail(final Exception exception) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                enableUI();
                                displayErrorMessage(exception);
                            }
                        });
                    }

                });

            }
        });
    }

    private void setupRelativeLayout() {

        int availableScreenHeight = this.getResources().getDisplayMetrics().heightPixels;

        float scale =  this.getResources().getDisplayMetrics().density;
        int sendMessageControlLayoutHeight = (int)(100 * scale + 0.5f);

        if (mKeyboardIsVisible) {
            availableScreenHeight -= (mKeyboardHeight - 100);
        }

        int recyclerViewHeight = availableScreenHeight - sendMessageControlLayoutHeight;

        mLayout1.getLayoutParams().height = availableScreenHeight;
        mRecyclerView.getLayoutParams().height = recyclerViewHeight - 210;
        mLayout1.requestLayout();
    }

    private void setupGlobalLayoutListener() {
        mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                mCoordinatorLayout.getWindowVisibleDisplayFrame(r);

                int screenHeight = mCoordinatorLayout.getRootView().getHeight();
                int keyboardHeight = screenHeight - (r.bottom);

                if (keyboardHeight > 150) {
                    mKeyboardHeight = keyboardHeight;
                    mKeyboardIsVisible = true;
                } else {
                    mKeyboardHeight = 0;
                    mKeyboardIsVisible = false;
                }

                setupRelativeLayout();
            }
        });
    }

    private void disableUI() {
        mMessageTextView.setEnabled(false);
        mUploadImageButton.setEnabled(false);
        mSendTextButton.setEnabled(false);
    }

    private void enableUI() {
        mMessageTextView.setEnabled(true);
        mUploadImageButton.setEnabled(true);
        mSendTextButton.setEnabled(true);
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

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUESTCODE_UPLOAD_ACTIVITY) {
            refreshMessages();
        }
    }

    public void didTapOnRowAtIndex(int selectedIndex) {
        // do nothing for now.
    }
}
