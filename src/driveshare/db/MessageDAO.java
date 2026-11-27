package driveshare.db;

import driveshare.model.Message;
import driveshare.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for the messages table. */
public class MessageDAO {

    public void insert(int senderId, int receiverId, String body) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, body, sent_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, body);
            ps.setString(4, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not send message: " + e.getMessage(), e);
        }
    }

    /** All messages between two users, oldest first. */
    public List<Message> findConversation(int userA, int userB) {
        String sql = "SELECT m.*, u.full_name AS sender_name FROM messages m JOIN users u ON u.id = m.sender_id"
                + " WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?)"
                + " ORDER BY m.id";
        List<Message> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Message(rs.getInt("id"), rs.getInt("sender_id"), rs.getInt("receiver_id"),
                            rs.getString("sender_name"), rs.getString("body"),
                            LocalDateTime.parse(rs.getString("sent_at"))));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Could not read messages: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * People the user can chat with: anyone they already exchanged messages with,
     * plus the other side of every booking (owner or renter).
     */
    public List<User> findContacts(int userId) {
        String sql = "SELECT id, email, full_name, balance FROM users WHERE id IN ("
                + "   SELECT receiver_id FROM messages WHERE sender_id = ?"
                + "   UNION SELECT sender_id FROM messages WHERE receiver_id = ?"
                + "   UNION SELECT c.owner_id FROM bookings b JOIN cars c ON c.id = b.car_id WHERE b.renter_id = ?"
                + "   UNION SELECT b.renter_id FROM bookings b JOIN cars c ON c.id = b.car_id WHERE c.owner_id = ?"
                + ") AND id <> ? ORDER BY full_name";
        List<User> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) ps.setInt(i, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new User(rs.getInt("id"), rs.getString("email"), rs.getString("full_name"),
                            rs.getDouble("balance")));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Could not read contacts: " + e.getMessage(), e);
        }
        return list;
    }
}
