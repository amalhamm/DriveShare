package driveshare.db;

import driveshare.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for the notifications table (in-app notifications). */
public class NotificationDAO {

    public void insert(int userId, String message) {
        String sql = "INSERT INTO notifications (user_id, message, created_at, is_read) VALUES (?, ?, ?, 0)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not save notification: " + e.getMessage(), e);
        }
    }

    /** All notifications of a user, newest first. */
    public List<Notification> findByUser(int userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Notification(rs.getInt("id"), rs.getInt("user_id"), rs.getString("message"),
                            LocalDateTime.parse(rs.getString("created_at")), rs.getInt("is_read") == 1));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Could not read notifications: " + e.getMessage(), e);
        }
        return list;
    }

    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataException("Could not count notifications: " + e.getMessage(), e);
        }
    }

    public void markAllRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not update notifications: " + e.getMessage(), e);
        }
    }
}
