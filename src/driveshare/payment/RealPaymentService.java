package driveshare.payment;

import driveshare.db.BookingDAO;
import driveshare.db.Database;
import driveshare.model.Booking;
import driveshare.model.BookingStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * PROXY PATTERN - Real Subject.
 *
 * The (simulated) payment system. No real money is used: it moves the amount from the
 * renter's balance to the owner's balance, records the payment and marks the booking PAID,
 * all in ONE database transaction (either everything happens or nothing does).
 *
 * The screens never use this class directly - they go through PaymentProxy, which
 * performs the security checks first.
 */
public class RealPaymentService implements PaymentService {

    private final BookingDAO bookingDAO = new BookingDAO();

    public RealPaymentService() {
        System.out.println("[RealPaymentService] Connected to the (simulated) payment system");
    }

    @Override
    public String pay(Booking booking) throws PaymentException {
        double amount = booking.getTotalPrice();
        String now = LocalDateTime.now().withNano(0).toString();

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. take the money from the renter
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET balance = balance - ? WHERE id = ?")) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, booking.getRenterId());
                    ps.executeUpdate();
                }
                // 2. give it to the owner
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET balance = balance + ? WHERE id = ?")) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, booking.getOwnerId());
                    ps.executeUpdate();
                }
                // 3. record the payment
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO payments (booking_id, payer_id, payee_id, amount, paid_at) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, booking.getId());
                    ps.setInt(2, booking.getRenterId());
                    ps.setInt(3, booking.getOwnerId());
                    ps.setDouble(4, amount);
                    ps.setString(5, now);
                    ps.executeUpdate();
                }
                // 4. mark the booking as paid
                bookingDAO.updateStatus(conn, booking.getId(), BookingStatus.PAID);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PaymentException("Payment failed: " + e.getMessage());
        }

        System.out.printf("[RealPaymentService] Transferred $%.2f for booking #%d%n", amount, booking.getId());
        return String.format("Payment of $%.2f for booking #%d (%s) completed on %s.",
                amount, booking.getId(), booking.getCarTitle(), now.replace('T', ' '));
    }
}
