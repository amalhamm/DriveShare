package driveshare.db;

import driveshare.model.Watch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for the watches table (renter watch list). */
public class WatchDAO {

    private static final String SELECT =
            "SELECT w.*, (c.year || ' ' || c.model) AS car_title FROM watches w JOIN cars c ON c.id = w.car_id ";

    /**
     * Adds a watch, or replaces the conditions if the renter already watches this car.
     * @return the saved watch
     */
    public Watch upsert(int renterId, int carId, Double maxPrice, boolean notifyWhenAvailable) {
        String sql = "INSERT INTO watches (renter_id, car_id, max_price, notify_when_available, created_at)"
                + " VALUES (?, ?, ?, ?, ?)"
                + " ON CONFLICT(renter_id, car_id) DO UPDATE SET"
                + "   max_price = excluded.max_price, notify_when_available = excluded.notify_when_available";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, renterId);
            ps.setInt(2, carId);
            if (maxPrice == null) ps.setNull(3, Types.REAL); else ps.setDouble(3, maxPrice);
            ps.setInt(4, notifyWhenAvailable ? 1 : 0);
            ps.setString(5, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not save watch: " + e.getMessage(), e);
        }
        List<Watch> saved = query(SELECT + "WHERE w.renter_id = ? AND w.car_id = ?", renterId, carId);
        return saved.get(0);
    }

    public void delete(int watchId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM watches WHERE id = ?")) {
            ps.setInt(1, watchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not remove watch: " + e.getMessage(), e);
        }
    }

    public List<Watch> findByRenter(int renterId) {
        return query(SELECT + "WHERE w.renter_id = ? ORDER BY w.id", renterId);
    }

    /** Every watch in the system - loaded at start-up to register the observers. */
    public List<Watch> findAll() {
        return query(SELECT + "ORDER BY w.id");
    }

    private List<Watch> query(String sql, Object... params) {
        List<Watch> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double max = rs.getDouble("max_price");
                    Double maxPrice = rs.wasNull() ? null : max;
                    list.add(new Watch(rs.getInt("id"), rs.getInt("renter_id"), rs.getInt("car_id"),
                            rs.getString("car_title"), maxPrice, rs.getInt("notify_when_available") == 1));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Could not read watches: " + e.getMessage(), e);
        }
        return list;
    }
}
