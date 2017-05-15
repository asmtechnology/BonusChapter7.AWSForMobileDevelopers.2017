package asmtechnology.com.awschat.controllers;

import android.content.Context;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import asmtechnology.com.awschat.interfaces.DynamoDBControllerGenericHandler;
import asmtechnology.com.awschat.interfaces.DynamoDBControllerRetrieveChatHandler;
import asmtechnology.com.awschat.interfaces.DynamoDBControllerRetrieveFriendIDsHandler;
import asmtechnology.com.awschat.interfaces.DynamoDBControllerRetrieveUserHandler;
import asmtechnology.com.awschat.models.Chat;
import asmtechnology.com.awschat.models.Friend;
import asmtechnology.com.awschat.models.Message;
import asmtechnology.com.awschat.models.User;

public class DynamoDBController {

    private Context mContext;

    private static DynamoDBController instance = null;
    private DynamoDBController() {}

    public static DynamoDBController getInstance(Context context) {
        if(instance == null) {
            instance = new DynamoDBController();
        }

        instance.mContext = context;
        return instance;
    }

    public void refreshFriendList(final String userId, final DynamoDBControllerGenericHandler completion) {

        Runnable runnable = new Runnable() {
            public void run() {

                retrieveFriendIds(userId, new DynamoDBControllerRetrieveFriendIDsHandler() {
                    @Override
                    public void didSucceed(ArrayList<String> results) {

                        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
                        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                        DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
                        try {
                            PaginatedScanList<User> users = mapper.scan(User.class, scanExpression);

                            // clear friend list in ChatManager
                            ChatManager chatManager = ChatManager.getInstance(mContext);
                            chatManager.clearFriendList();

                            // add User objects.
                            for (User u : users) {

                                if (results.contains(u.getId())) {
                                    chatManager.addFriend(u);
                                }
                            }

                            completion.didSucceed();

                        } catch (AmazonServiceException ex) {
                            completion.didFail(ex);
                        }
                    }

                    @Override
                    public void didFail(Exception exception) {
                        completion.didFail(exception);
                    }
                });

            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void retrieveUser (final String userId, final DynamoDBControllerRetrieveUserHandler completion) {

        Runnable runnable = new Runnable() {

            public void run() {
                CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
                AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                try {
                    User user = mapper.load(User.class, userId);

                    completion.didSucceed(user);

                } catch (AmazonServiceException ex) {
                    completion.didFail(ex);
                }
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();
    }


    public void refreshPotentialFriendList(final String currentUserId, final DynamoDBControllerGenericHandler completion) {

        Runnable runnable = new Runnable() {

            public void run() {

                retrieveFriendIds(currentUserId, new DynamoDBControllerRetrieveFriendIDsHandler() {
                    @Override
                    public void didSucceed(ArrayList<String> results) {
                        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
                        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                        DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
                        try {
                            PaginatedScanList<User> users = mapper.scan(User.class, scanExpression);

                            // clear potential friend list in ChatManager
                            ChatManager chatManager = ChatManager.getInstance(mContext);
                            chatManager.clearPotentialFriendList();

                            // add users who are not friends.
                            for (User u : users) {

                                if (results.contains(u.getId())) {
                                    continue;
                                }

                                if (u.getId().equals(currentUserId)) {
                                    continue;
                                }

                                chatManager.addPotentialFriend(u);
                            }

                            completion.didSucceed();

                        } catch (AmazonServiceException ex) {
                            completion.didFail(ex);
                        }
                    }

                    @Override
                    public void didFail(Exception exception) {
                        completion.didFail(exception);
                    }
                });
            }
        };

        Thread mythread = new Thread(runnable);
        mythread.start();

    }

    public void addFriend(final String currentUserId,
                   final String friendUserId,
                   final DynamoDBControllerGenericHandler completion) {

        Runnable runnable = new Runnable() {
            public void run() {

                try {
                    CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);

                    AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                    DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                    Friend friendRelationship = new Friend();
                    friendRelationship.setId(generateUUID());
                    friendRelationship.setUser_id(currentUserId);
                    friendRelationship.setFriend_id(friendUserId);

                    mapper.save(friendRelationship);
                    completion.didSucceed();
                } catch (AmazonServiceException ex) {
                    completion.didFail(ex);
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    private String generateUUID() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        return uuidString.toUpperCase();
    }

    private void retrieveFriendIds(String userId, DynamoDBControllerRetrieveFriendIDsHandler completion) {

        CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
        AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
        DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

        Map<String, AttributeValue> attributeValues = new HashMap<String, AttributeValue>();
        attributeValues.put(":val1", new AttributeValue().withS(userId));

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("user_id = :val1")
                .withExpressionAttributeValues(attributeValues);

        try {
            PaginatedScanList<Friend> results = mapper.scan(Friend.class, scanExpression);

            ArrayList<String> friendUserIdList = new ArrayList<String>();

            for (Friend f : results) {
                friendUserIdList.add(f.getFriend_id());
            }

            completion.didSucceed(friendUserIdList);

        } catch (AmazonServiceException ex) {
           completion.didFail(ex);
        }

    }


    public void retrieveChat(final String fromUserId, final String toUserId, final DynamoDBControllerRetrieveChatHandler completion){

        Runnable runnable = new Runnable() {
            public void run() {

                String chatID = fromUserId + toUserId;
                String alternateChatID = toUserId + fromUserId;

                ChatManager chatManager = ChatManager.getInstance(mContext);

                CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
                AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                try {
                    Chat chat = mapper.load(Chat.class, chatID);
                    if (chat != null) {
                        chatManager.addChat(chat);
                        completion.didSucceed();
                        return;
                    }

                    Chat alternateChat = mapper.load(Chat.class, alternateChatID);
                    if (alternateChat != null) {
                        chatManager.addChat(alternateChat);
                        completion.didSucceed();
                        return;
                    }

                    completion.didNotFindChat();

                } catch (AmazonServiceException ex) {
                    completion.didFail(ex);
                }

            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }


    public void createChat(final String fromUserId,
                          final String toUserId,
                          final DynamoDBControllerGenericHandler completion) {

        Runnable runnable = new Runnable() {
            public void run() {

                String newChatId = fromUserId + toUserId;
                ChatManager chatManager = ChatManager.getInstance(mContext);

                try {
                    CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);

                    AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                    DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                    Chat chat = new Chat();
                    chat.setId(newChatId);
                    chat.setFrom_user_id(fromUserId);
                    chat.setTo_user_id(toUserId);
                    mapper.save(chat);

                    chatManager.addChat(chat);
                    completion.didSucceed();

                } catch (AmazonServiceException ex) {
                    completion.didFail(ex);
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void sendTextMessage(final String fromUserId,
                                final String chatId,
                                final String messageText,
                                final DynamoDBControllerGenericHandler completion) {

        Runnable runnable = new Runnable() {
            public void run() {

                long timeInMillisecondsSince1970 = new Date().getTime();
                long timeInSecondsSince1970 = (long) (timeInMillisecondsSince1970 / 1000L);
                ChatManager chatManager = ChatManager.getInstance(mContext);

                try {
                    CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);

                    AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                    DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                    Message message = new Message();
                    message.setChat_id(chatId);
                    message.setDate_sent((double)timeInSecondsSince1970);
                    message.setMessage_id(generateUUID());
                    message.setMessage_text(messageText);
                    message.setMessage_image("NA");
                    message.setMesage_image_preview("NA");
                    message.setSender_id(fromUserId);
                    mapper.save(message);

                    chatManager.addMessage(chatId, message);
                    completion.didSucceed();

                } catch (AmazonServiceException ex) {
                    completion.didFail(ex);
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }


    public void sendImage(final String fromUserId,
                          final String chatId,
                          final String imageFile,
                          final String previewFile,
                          final DynamoDBControllerGenericHandler completion) {

        Runnable runnable = new Runnable() {
            public void run() {

                long timeInMillisecondsSince1970 = new Date().getTime();
                long timeInSecondsSince1970 = (long) (timeInMillisecondsSince1970 / 1000L);
                ChatManager chatManager = ChatManager.getInstance(mContext);

                try {
                    CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);

                    AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                    DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                    Message message = new Message();
                    message.setChat_id(chatId);
                    message.setDate_sent((double)timeInSecondsSince1970);
                    message.setMessage_id(generateUUID());
                    message.setMessage_text("NA");
                    message.setMessage_image(imageFile);
                    message.setMesage_image_preview(previewFile);
                    message.setSender_id(fromUserId);
                    mapper.save(message);

                    chatManager.addMessage(chatId, message);
                    completion.didSucceed();

                } catch (AmazonServiceException ex) {
                    completion.didFail(ex);
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }

    public void retrieveAllMessages(final String chatId, final Date fromDate, final DynamoDBControllerGenericHandler completion) {

        Runnable runnable = new Runnable() {
            public void run() {


                long fromDateInMillisecondsSince1970 = fromDate.getTime();
                long fromDateInSecondsSince1970 = (long) (fromDateInMillisecondsSince1970 / 1000L);

                Condition rangeKeyCondition = new Condition()
                        .withComparisonOperator(ComparisonOperator.GT.toString())
                        .withAttributeValueList(new AttributeValue().withN(Long.toString(fromDateInSecondsSince1970)));

                Message messageKey = new Message();
                messageKey.setChat_id(chatId);

                DynamoDBQueryExpression<Message> queryExpression = new DynamoDBQueryExpression<Message>()
                        .withHashKeyValues(messageKey)
                        .withRangeKeyCondition("date_sent", rangeKeyCondition);

                ChatManager chatManager = ChatManager.getInstance(mContext);

                CognitoIdentityPoolController identityPoolController = CognitoIdentityPoolController.getInstance(mContext);
                AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(identityPoolController.mCredentialsProvider);
                DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

                try {
                    List<Message> messages = mapper.query(Message.class, queryExpression);
                    for (Message m : messages) {
                        chatManager.addMessage(chatId, m);
                    }
                    completion.didSucceed();
                } catch (AmazonServiceException ex) {
                    completion.didFail(ex);
                }
            }
        };
        Thread mythread = new Thread(runnable);
        mythread.start();
    }


}
