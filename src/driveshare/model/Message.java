package driveshare.model;

import java.time.LocalDateTime;

/** A chat message between two users (usually a car owner and a renter). */
public class Message {

    private final int id;
    private final int senderId;
    private final int receiverId;
    private final String senderName;
    private final String body;
    private final LocalDateTime sentAt;

    public Message(int id, int senderId, int receiverId, String senderName, String body, LocalDateTime sentAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderName = senderName;
        this.body = body;
        this.sentAt = sentAt;
    }

    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getSenderName() { return senderName; }
    public String getBody() { return body; }
    public LocalDateTime getSentAt() { return sentAt; }
}
