package asmtechnology.com.awschat.recyclerview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import asmtechnology.com.awschat.R;
import asmtechnology.com.awschat.controllers.ChatManager;
import asmtechnology.com.awschat.interfaces.RecyclerViewHolderListener;
import asmtechnology.com.awschat.models.User;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {

    private Context mContext;
    private RecyclerViewHolderListener mListener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public TextView emailAddress;
        public int itemIndex;

        public ViewHolder(View view) {
            super(view);
            username = (TextView) view.findViewById(R.id.friend_username);
            emailAddress = (TextView) view.findViewById(R.id.friend_emailaddress);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.didTapOnRowAtIndex(itemIndex);
                }
            });
        }
    }

    public FriendListAdapter(Context context, RecyclerViewHolderListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.friend_list_row, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        ChatManager chatManager = ChatManager.getInstance(mContext);

        User user = chatManager.friendList.get(position);

        holder.username.setText(user.getUsername());
        holder.emailAddress.setText(user.getEmail_address());
        holder.itemIndex = position;
    }

    @Override
    public int getItemCount() {
        ChatManager chatManager = ChatManager.getInstance(mContext);
        return chatManager.friendList.size();
    }

}
