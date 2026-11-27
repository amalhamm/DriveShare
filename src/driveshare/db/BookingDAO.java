package driveshare.db;

import driveshare.model.Booking;
import driveshare.model.BookingStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Data access for the bookings table. */
public class BookingDAO {

    private static final String SELECT =
            "SELECT b.*, c.owner_id, (c.year || ' ' || c.model) AS car_title,"
            + " r.full_name AS renter_name, o.full_name AS owner_name"
            + " FROM bookings b"
            + " JOIN cars c  ON c.id = b.car_id"
            + " JOIN users r ON r.id = b.renter_id"
            + " JOIN users o ON o.id = c.owner_id ";

    /** SQL condition for bookings that still hold the car (see BookingStatus.blocksCar). */
    private static final String ACTIVE = "('PENDING','CONFIRMED','PAID')";

    /**
     * True if the car already has an active booking that overlaps [start, end].
     * Two inclusive ranges overlap when newStart <= oldEnd AND newEnd >= oldStart.
     */
    public boolean hasOverlap(int carId, LocalDate start, LocalDate end) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE car_id = ? AND status IN " + ACTIVE
                + " AND ? <= end_date AND ? >= start_date";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, carId);
            ps.setString(2, start.toString());
            ps.setString(3, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataException("Could not check availability: " + e.getMessage(), e);
        }
    }

    /**
     * Inserts a PENDING booking. The trigger "prevent_overlapping_bookings" makes the
     * database itself reject the row if another active booking overlaps.
     */
    public int insert(int carId, int renterId, LocalDate start, LocalDate end, double total) {
        String sql = "INSERT INTO bookings (car_id, renter_id, start_date, end_date, total_price, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, 'PENDING', ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, carId);
            ps.setInt(2, renterId);
            ps.setString(3, start.toString());
            ps.setString(4, end.toString());
            ps.setDouble(5, total);
            ps.setString(6, LocalDateTime.now().withNano(0).toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataException(e.getMessage(), e);
        }
    }

    public void updateStatus(int bookingId, BookingStatus status) {
        try (Connection conn = Database.getConnection()) {
            updateStatus(conn, bookingId, status);
        } catch (SQLException e) {
            throw new DataException("Could not update booking: " + e.getMessage(), e);
        }
    }

    /** Version that joins an existing transaction (used by the payment service). */
    public void updateStatus(Connection conn, int bookingId, BookingStatus status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE bookings SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    public Booking findById(int id) {
        List<Booking> list = query(SELECT + "WHERE b.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Trips the user booked as a renter, newest first. */
    public List<Booking> findByRenter(int renterId) {
        return query(SELECT + "WHERE b.renter_id = ? ORDER BY b.id DESC", renterId);
    }

    /** Bookings made on cars the user owns, newest first. */
    public List<Booking> findByOwner(int ownerId) {
        return query(SELECT + "WHERE c.owner_id = ? ORDER BY b.id DESC", ownerId);
    }

    /** Active bookings of one car - used to draw the availability calendar. */
    public List<Booking> findActiveByCar(int carId) {
        return query(SELECT + "WHERE b.car_id = ? AND b.status IN " + ACTIVE + " ORDER BY b.start_date", carId);
    }

    private List<Booking> query(String sql, Object param) {
        List<Booking> list = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Booking(
                            rs.getInt("id"), rs.getInt("car_id"), rs.getInt("renter_id"), rs.getInt("owner_id"),
                            rs.getString("car_title"), rs.getString("renter_name"), rs.getString("owner_name"),
                            LocalDate.parse(rs.getString("start_date")), LocalDate.parse(rs.getString("end_date")),
                            rs.getDouble("total_price"), BookingStatus.valueOf(rs.getString("status"))));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Could not read bookings: " + e.getMessage(), e);
        }
        return list;
    }
}
