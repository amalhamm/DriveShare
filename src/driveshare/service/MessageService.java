package driveshare.service;

import driveshare.auth.SessionManager;
import driveshare.db.MessageDAO;
import driveshare.db.UserDAO;
import driveshare.model.Message;
import driveshare.model.User;

import java.util.List;

/** Messaging between car owners and renters. The receiver also gets an in-app notification. */
public class MessageService {

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationService notifications = new NotificationService();

    public void send(int receiverId, String body) {
        User sender = SessionManager.getInstance().requireUser();
        if (body == null || body.trim().isEmpty())
            throw new IllegalArgumentException("Message cannot be empty.");
        if (receiverId == sender.getId())
            throw new IllegalArgumentException("You cannot message yourself.");

        String text = body.trim();
        messageDAO.insert(sender.getId(), receiverId, text);
        String preview = text.length() > 60 ? text.substring(0, 57) + "..." : text;
        notifications.notifyUser(receiverId, "New message from " + sender.getFullName() + ": \"" + preview + "\"");
    }

    public List<Message> getConversation(int otherUserId) {
        return messageDAO.findConversation(SessionManager.getInstance().requireUser().getId(), otherUserId);
    }

    public List<User> getContacts() {
        return messageDAO.findContacts(SessionManager.getInstance().requireUser().getId());
    }

    public User findUser(int userId) {
        return userDAO.findById(userId);
    }
}
