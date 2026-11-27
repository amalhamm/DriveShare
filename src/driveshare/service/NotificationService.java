package driveshare.service;

import driveshare.db.NotificationDAO;
import driveshare.model.Notification;

import java.util.List;

/**
 * Sends in-app notifications (booking requests, confirmations, payments, watch alerts,
 * new messages). The user sees them on the Notifications tab and as a counter in the header.
 */
public class NotificationService {

    private final NotificationDAO dao = new NotificationDAO();

    public void notifyUser(int userId, String message) {
        dao.insert(userId, message);
    }

    public List<Notification> getNotifications(int userId) {
        return dao.findByUser(userId);
    }

    public int countUnread(int userId) {
        return dao.countUnread(userId);
    }

    public void markAllRead(int userId) {
        dao.markAllRead(userId);
    }
}
