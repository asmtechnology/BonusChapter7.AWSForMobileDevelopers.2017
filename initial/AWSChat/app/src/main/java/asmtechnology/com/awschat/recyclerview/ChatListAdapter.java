package asmtechnology.com.awschat.recyclerview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import asmtechnology.com.awschat.R;
import asmtechnology.com.awschat.controllers.ChatManager;
import asmtechnology.com.awschat.controllers.S3Controller;
import asmtechnology.com.awschat.interfaces.RecyclerViewHolderListener;
import asmtechnology.com.awschat.interfaces.S3ControllerGenericHandler;
import asmtechnology.com.awschat.models.Chat;
import asmtechnology.com.awschat.models.Message;

public class ChatListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Activity mActivity;
    private Context mContext;
    private RecyclerViewHolderListener mListener;
    private Chat mChat;
    private String mCurrentUserId;

    private int SENT_TEXT_VIEW = 0;
    private int SENT_IMAGE_VIEW = 1;
    private int RECEIVED_TEXT_VIEW = 2;
    private int RECEIVED_IMAGE_VIEW = 3;

    public class SentTextViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public int itemIndex;

        public SentTextViewHolder(View view) {
            super(view);
            messageText = (TextView) view.findViewById(R.id.message_text);
        }
    }

    public class SentImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public int itemIndex;

        public SentImageViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.imageView);
        }

        public void loadImage(final String imageName, final Activity activity) {

            String externalStorageDirectory = Environment.getExternalStorageDirectory().toString();
            final String localFilePath = externalStorageDirectory + "/" + imageName + ".png";
            File file = new File(localFilePath);

            // image exists locally. use local copy.
            if (file.exists()){
                Bitmap b = BitmapFactory.decodeFile(localFilePath);
                imageView.setImageBitmap(b);
                return;
            }

            // image does not exist locally,
            // download from S3 and save to documents directory.
            imageView.setImageResource(R.drawable.placeholder);

            S3Controller s3Controller = S3Controller.getInstance(mContext);
            s3Controller.downloadThumbnail(localFilePath, imageName, new S3ControllerGenericHandler() {
                @Override
                public void didSucceed() {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap b = BitmapFactory.decodeFile(localFilePath);
                            imageView.setImageBitmap(b);
                        }
                    });
                }

                @Override
                public void didFail(Exception exception) {
                    Log.e("AWSCHAT", "Failed to download remote image:" + imageName, exception);
                }
            });
        }

    }

    public class ReceivedTextViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public int itemIndex;

        public ReceivedTextViewHolder(View view) {
            super(view);
            messageText = (TextView) view.findViewById(R.id.message_text);
        }
    }

    public class ReceivedImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public int itemIndex;

        public ReceivedImageViewHolder(View view) {
            super(view);
            imageView = (ImageView) view.findViewById(R.id.imageView);
        }

        public void loadImage(String imageName, final Activity activity) {

        }
    }

    public ChatListAdapter(Context context, RecyclerViewHolderListener listener, Chat chat, String currentUserId, Activity activity) {
        mContext = context;
        mListener = listener;
        mCurrentUserId = currentUserId;
        mChat = chat;
        mActivity = activity;
    }

    public void setChat(Chat c) {
        mChat = c;
    }

    @Override
    public int getItemViewType(int position) {

        if (mChat == null) {
            return SENT_TEXT_VIEW;
        }

        ChatManager chatManager = ChatManager.getInstance(mContext);
        ArrayList<Message> messages = chatManager.conversations.get(mChat);

        Message message = messages.get(position);
        String messageText = message.getMessage_text();
        String senderId = message.getSender_id();

        if (messageText.equals("NA")) {
            // image
            if (senderId.equals(mCurrentUserId)) {
                return SENT_IMAGE_VIEW;
            } else {
                return RECEIVED_IMAGE_VIEW;
            }
        } else {
            // text
            if (senderId.equals(mCurrentUserId)) {
                return SENT_TEXT_VIEW;
            } else {
                return RECEIVED_TEXT_VIEW;
            }
        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == SENT_TEXT_VIEW) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sent_text_row, parent, false);
            return new ChatListAdapter.SentTextViewHolder(itemView);

        } else if (viewType == SENT_IMAGE_VIEW) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sent_image_row, parent, false);
            return new ChatListAdapter.SentImageViewHolder(itemView);

        } else if (viewType == RECEIVED_TEXT_VIEW) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.received_text_row, parent, false);
            return new ChatListAdapter.ReceivedTextViewHolder(itemView);

        } else if (viewType == RECEIVED_IMAGE_VIEW) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.received_image_row, parent, false);
            return new ChatListAdapter.ReceivedImageViewHolder(itemView);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (mChat == null) {
            return;
        }

        ChatManager chatManager = ChatManager.getInstance(mContext);
        ArrayList<Message> messages = chatManager.conversations.get(mChat);

        Message message = messages.get(position);
        String messageText = message.getMessage_text();
        String senderId = message.getSender_id();
        String messageImagePreview = message.getMesage_image_preview();

        if (holder.getItemViewType() == SENT_TEXT_VIEW) {
            ((SentTextViewHolder) holder).itemIndex = position;
            ((SentTextViewHolder) holder).messageText.setText(messageText);

        } else if (holder.getItemViewType() == SENT_IMAGE_VIEW) {
            ((SentImageViewHolder) holder).itemIndex = position;
            ((SentImageViewHolder) holder).loadImage(messageImagePreview, mActivity);

        } else if (holder.getItemViewType() == RECEIVED_TEXT_VIEW) {
            ((ReceivedTextViewHolder) holder).itemIndex = position;
            ((ReceivedTextViewHolder) holder).messageText.setText(messageText);

        } else if (holder.getItemViewType() == RECEIVED_IMAGE_VIEW) {
            ((ReceivedImageViewHolder) holder).itemIndex = position;
            ((ReceivedImageViewHolder) holder).loadImage(messageImagePreview, mActivity);
        }

    }

    @Override
    public int getItemCount() {

        if (mChat == null) {
            return 0;
        }

        ChatManager chatManager = ChatManager.getInstance(mContext);
        ArrayList<Message> messages = chatManager.conversations.get(mChat);

        return messages.size();
    }
}
