package driveshare.db;

import driveshare.model.SecurityQuestion;
import driveshare.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for the users and security_questions tables. */
public class UserDAO {

    /**
     * Inserts a user and the three security questions in one transaction.
     * @return the new user's id
     */
    public int insertUser(String email, String fullName, String passwordHash, String passwordSalt,
                          double balance, List<SecurityQuestion> questions) {
        String userSql = "INSERT INTO users (email, full_name, password_hash, password_salt, balance, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        String questionSql = "INSERT INTO security_questions (user_id, question_order, question, answer_hash, answer_salt)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement ps = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, email);
                    ps.setString(2, fullName);
                    ps.setString(3, passwordHash);
                    ps.setString(4, passwordSalt);
                    ps.setDouble(5, balance);
                    ps.setString(6, LocalDateTime.now().withNano(0).toString());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(questionSql)) {
                    for (SecurityQuestion q : questions) {
                        ps.setInt(1, userId);
                        ps.setInt(2, q.getOrder());
                        ps.setString(3, q.getQuestion());
                        ps.setString(4, q.getAnswerHash());
                        ps.setString(5, q.getAnswerSalt());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return userId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataException("Could not save user: " + e.getMessage(), e);
        }
    }

    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }

    /** @return the user, or null if no account uses this email */
    public User findByEmail(String email) {
        return findOne("SELECT id, email, full_name, balance FROM users WHERE lower(email) = lower(?)", email);
    }

    public User findById(int id) {
        return findOne("SELECT id, email, full_name, balance FROM users WHERE id = ?", id);
    }

    /** @return {hash, salt} of the stored password, or null if the email is unknown */
    public String[] getPasswordHashAndSalt(String email) {
        String sql = "SELECT password_hash, password_salt FROM users WHERE lower(email) = lower(?)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new String[] { rs.getString(1), rs.getString(2) } : null;
            }
        } catch (SQLException e) {
            throw new DataException("Could not read password: " + e.getMessage(), e);
        }
    }

    public void updatePassword(int userId, String hash, String salt) {
        String sql = "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not update password: " + e.getMessage(), e);
        }
    }

    /** @return the user's three security questions ordered 1, 2, 3 */
    public List<SecurityQuestion> getSecurityQuestions(int userId) {
        String sql = "SELECT user_id, question_order, question, answer_hash, answer_salt"
                + " FROM security_questions WHERE user_id = ? ORDER BY question_order";
        List<SecurityQuestion> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SecurityQuestion(rs.getInt(1), rs.getInt(2), rs.getString(3),
                            rs.getString(4), rs.getString(5)));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Could not read security questions: " + e.getMessage(), e);
        }
        return list;
    }

    private User findOne(String sql, Object param) {
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new User(rs.getInt("id"), rs.getString("email"), rs.getString("full_name"),
                        rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            throw new DataException("Could not read user: " + e.getMessage(), e);
        }
    }
}
