package driveshare.payment;

import driveshare.auth.SessionManager;
import driveshare.db.BookingDAO;
import driveshare.db.UserDAO;
import driveshare.model.Booking;
import driveshare.model.BookingStatus;
import driveshare.model.User;
import driveshare.service.NotificationService;

/**
 * PROXY PATTERN - Proxy.
 *
 * Stands in front of RealPaymentService and simulates a secure interaction with it:
 *   1. Protection: refuses the payment unless the user is logged in (SessionManager), is the
 *      renter of the booking, the booking is CONFIRMED, the amount is correct and the
 *      renter has enough balance.
 *   2. Lazy creation: the real payment system is only created the first time it is needed.
 *   3. Logging + notifications: logs every attempt and, after a successful payment,
 *      notifies both the renter and the owner.
 *
 * Because it implements the same PaymentService interface, the Bookings screen does not
 * know whether it talks to the proxy or to the real service.
 */
public class PaymentProxy implements PaymentService {

    private RealPaymentService realService;          // created lazily
    private final BookingDAO bookingDAO = new BookingDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationService notificationService = new NotificationService();

    @Override
    public String pay(Booking booking) throws PaymentException {
        System.out.println("[PaymentProxy] Payment requested for booking #" + booking.getId());

        // ---- security checks before the real system is touched ----
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) {
            throw new PaymentException("You must be logged in to pay.");
        }
        User payer = session.getCurrentUser();

        // always re-read the booking from the database, never trust the copy on screen
        Booking current = bookingDAO.findById(booking.getId());
        if (current == null) {
            throw new PaymentException("Booking not found.");
        }
        if (current.getRenterId() != payer.getId()) {
            throw new PaymentException("Only the renter of this booking can pay for it.");
        }
        if (current.getStatus() == BookingStatus.PAID) {
            throw new PaymentException("This booking is already paid.");
        }
        if (current.getStatus() != BookingStatus.CONFIRMED) {
            throw new PaymentException("The owner must confirm the booking before you can pay.");
        }
        if (current.getTotalPrice() <= 0) {
            throw new PaymentException("Invalid payment amount.");
        }
        User freshPayer = userDAO.findById(payer.getId());
        if (freshPayer.getBalance() < current.getTotalPrice()) {
            throw new PaymentException(String.format("Insufficient balance: you have $%.2f but need $%.2f.",
                    freshPayer.getBalance(), current.getTotalPrice()));
        }

        // ---- delegate to the real subject (created only when first needed) ----
        if (realService == null) {
            realService = new RealPaymentService();
        }
        String receipt = realService.pay(current);

        // ---- after the payment: refresh the session balance and notify both sides ----
        session.refreshUser();
        notificationService.notifyUser(current.getRenterId(), String.format(
                "Payment sent: $%.2f for the %s (%s to %s). Your new balance is $%.2f.",
                current.getTotalPrice(), current.getCarTitle(), current.getStartDate(), current.getEndDate(),
                session.getCurrentUser().getBalance()));
        notificationService.notifyUser(current.getOwnerId(), String.format(
                "Payment received: $%.2f from %s for the %s (%s to %s).",
                current.getTotalPrice(), current.getRenterName(), current.getCarTitle(),
                current.getStartDate(), current.getEndDate()));

        System.out.println("[PaymentProxy] Payment approved and completed");
        return receipt;
    }
}
