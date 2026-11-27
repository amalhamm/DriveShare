package driveshare.model;

import java.time.LocalDateTime;

/**
 * An in-app notification shown on the Notifications tab
 * (booking requests, confirmations, payments, watched-car alerts, new messages).
 */
public class Notification {

    private final int id;
    private final int userId;
    private final String message;
    private final LocalDateTime createdAt;
    private final boolean read;

    public Notification(int id, int userId, String message, LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.createdAt = createdAt;
        this.read = read;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
}
