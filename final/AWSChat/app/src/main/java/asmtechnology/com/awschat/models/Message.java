package asmtechnology.com.awschat.models;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "Message")
public class Message {

    private String chat_id;
    private Double date_sent;
    private String message_id;
    private String message_text;
    private String message_image;
    private String mesage_image_preview;
    private String sender_id;

    @DynamoDBHashKey(attributeName = "chat_id")
    public String getChat_id() {
        return chat_id;
    }

    public void setChat_id(String chat_id) {
        this.chat_id = chat_id;
    }

    @DynamoDBRangeKey(attributeName = "date_sent")
    public Double getDate_sent() {
        return date_sent;
    }

    public void setDate_sent(Double date_sent) {
        this.date_sent = date_sent;
    }

    @DynamoDBAttribute(attributeName = "message_id")
    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    @DynamoDBAttribute(attributeName = "message_text")
    public String getMessage_text() {
        return message_text;
    }

    public void setMessage_text(String message_text) {
        this.message_text = message_text;
    }

    @DynamoDBAttribute(attributeName = "message_image")
    public String getMessage_image() {
        return message_image;
    }

    public void setMessage_image(String message_image) {
        this.message_image = message_image;
    }

    @DynamoDBAttribute(attributeName = "mesage_image_preview")
    public String getMesage_image_preview() {
        return mesage_image_preview;
    }

    public void setMesage_image_preview(String mesage_image_preview) {
        this.mesage_image_preview = mesage_image_preview;
    }

    @DynamoDBAttribute(attributeName = "sender_id")
    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
    }

}
