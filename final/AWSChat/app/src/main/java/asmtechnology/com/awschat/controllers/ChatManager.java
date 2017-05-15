package asmtechnology.com.awschat.controllers;

import android.content.Context;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import asmtechnology.com.awschat.interfaces.ChatManagerGenericHandler;
import asmtechnology.com.awschat.interfaces.ChatManagerLoadChatHandler;
import asmtechnology.com.awschat.interfaces.DynamoDBControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.DynamoDBControllerRetrieveChatHandler;
import asmtechnology.com.awschat.interfaces.S3ControllerGenericHandler;
import asmtechnology.com.awschat.models.Chat;
import asmtechnology.com.awschat.models.Message;
import asmtechnology.com.awschat.models.User;

public class ChatManager {

    public HashMap<Chat, ArrayList<Message>> conversations;
    public ArrayList<User> friendList;
    public ArrayList<User> potentialFriendList;

    private Context mContext;

    private static ChatManager instance = null;
    private ChatManager() {}

    public static ChatManager getInstance(Context context) {
        if(instance == null) {
            instance = new ChatManager();
            instance.friendList = new ArrayList<User>();
            instance.potentialFriendList = new ArrayList<User>();
            instance.conversations = new HashMap<Chat, ArrayList<Message>>();
        }

        instance.mContext = context;
        return instance;
    }

    public void clearFriendList() {
        friendList.clear();
    }

    public void  addFriend(User user) {
        friendList.add(user);
    }

    public void  clearPotentialFriendList() {
        potentialFriendList.clear();
    }

    public void  addPotentialFriend(User user) {
        potentialFriendList.add(user);
    }

    public void clearCurrentChatList() {
        conversations.clear();
    }

    public void addChat(Chat chat) {

        Chat c = findChat(chat.getId());
        if (c != null) {
            return;
        }

        conversations.put(chat, new ArrayList<Message>());
    }

    public void addMessage(String chatId, Message message) {
        Chat chat = findChat(chatId);
        if (chat == null) {
            return;
        }

        ArrayList<Message> messages = conversations.get(chat);
        for (Message existingMessage : messages) {

            String existingMessageId = existingMessage.getMessage_id();
            if (existingMessageId.equals(message.getMessage_id())){
                return;
            }
        }

        messages.add(message);

    }

    public void loadChat(final String fromUserId, final String toUserId, final ChatManagerLoadChatHandler completion){

        Chat existingChat = findChat(fromUserId, toUserId);
        if (existingChat != null) {
            completion.didSucceed(existingChat);
            return;
        }

        final DynamoDBController dynamoDBController = DynamoDBController.getInstance(mContext);
        dynamoDBController.retrieveChat(fromUserId, toUserId, new DynamoDBControllerRetrieveChatHandler() {
            @Override
            public void didSucceed() {
                Chat c = findChat(fromUserId, toUserId);
                completion.didSucceed(c);
            }

            @Override
            public void didNotFindChat() {
                // no existing chat in dynamoDB, create one.
                dynamoDBController.createChat(fromUserId, toUserId, new DynamoDBControllerGenericHandler() {
                    @Override
                    public void didSucceed() {
                        Chat c = findChat(fromUserId, toUserId);
                        completion.didSucceed(c);
                    }

                    @Override
                    public void didFail(Exception exception) {
                        completion.didFail(exception);
                    }
                });
            }

            @Override
            public void didFail(Exception exception) {
                completion.didFail(exception);
            }
        });

    }

    public void refreshAllMessages(Chat chat, final ChatManagerGenericHandler completion) {

        Date earliestDate = new Date(0);

        DynamoDBController dynamoDBController = DynamoDBController.getInstance(mContext);
        dynamoDBController.retrieveAllMessages(chat.getId(), earliestDate, new DynamoDBControllerGenericHandler() {
            @Override
            public void didSucceed() {
                completion.didSucceed();
            }

            @Override
            public void didFail(Exception exception) {
                completion.didFail(exception);
            }
        });
    }


    public void sendTextMessage(final Chat chat, String messageText, final ChatManagerGenericHandler completion) {
        final Date timeSent = new Date();

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
        String senderID = identityPoolController.mCredentialsProvider.getIdentityId();

        final DynamoDBController dynamoDBController = DynamoDBController.getInstance(mContext);
        dynamoDBController.sendTextMessage(senderID, chat.getId(), messageText, new DynamoDBControllerGenericHandler() {
            @Override
            public void didSucceed() {
                dynamoDBController.retrieveAllMessages(chat.getId(), timeSent, new DynamoDBControllerGenericHandler() {
                    @Override
                    public void didSucceed() {
                        completion.didSucceed();
                    }

                    @Override
                    public void didFail(Exception exception) {
                        completion.didFail(exception);
                    }
                });
            }

            @Override
            public void didFail(Exception exception) {
                completion.didFail(exception);
            }
        });
    }

    public void sendImage(final Chat chat, String localFilePath, final ChatManagerGenericHandler completion){

        String extension = FilenameUtils.getExtension(localFilePath).toLowerCase();
        String uuid = generateUUID();

        final String imageFile = uuid;
        final String previewFile = "thumbnail-" + imageFile;

        final Date timeSent = new Date();

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
        final String senderID = identityPoolController.mCredentialsProvider.getIdentityId();

        S3Controller s3Controller = S3Controller.getInstance(mContext);
        s3Controller.uploadImage(localFilePath, imageFile, extension, new S3ControllerGenericHandler() {
            @Override
            public void didSucceed() {
                final DynamoDBController dynamoDBController = DynamoDBController.getInstance(mContext);
                dynamoDBController.sendImage(senderID, chat.getId(), imageFile, previewFile, new DynamoDBControllerGenericHandler() {
                    @Override
                    public void didSucceed() {

                        dynamoDBController.retrieveAllMessages(chat.getId(), timeSent, new DynamoDBControllerGenericHandler() {
                            @Override
                            public void didSucceed() {
                                completion.didSucceed();
                            }

                            @Override
                            public void didFail(Exception exception) {
                                completion.didFail(exception);
                            }
                        });
                    }

                    @Override
                    public void didFail(Exception exception) {
                        completion.didFail(exception);
                    }
                });
            }

            @Override
            public void didFail(Exception exception) {
                completion.didFail(exception);
            }
        });

    }


    private String generateUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        return uuidString.toUpperCase();
    }

    private Chat findChat(String chatId) {
        Iterator<Chat> it = conversations.keySet().iterator();
        while(it.hasNext()){
            Chat c = it.next();
            if (c.getId().equals(chatId)) {
                return c;
            }
        }

        return null;
    }

    private Chat findChat(String fromUserId, String toUserId) {
        Iterator<Chat> it = conversations.keySet().iterator();
        while(it.hasNext()){
            Chat c = it.next();

            String fromId = c.getFrom_user_id();
            String toId = c.getTo_user_id();

            if (((fromId.equals(fromUserId)) &&  (toId.equals(toUserId))) ||
               ((fromId.equals(toUserId)) && (toId.equals(fromUserId)))) {
                return c;
            }
        }

        return null;
    }

}
