package driveshare.payment;

import driveshare.model.Booking;

/**
 * PROXY PATTERN - Subject interface.
 *
 * Both the real payment system and its proxy implement this interface, so the screens
 * call pay(...) the same way without knowing that a proxy sits in front of the real system.
 */
public interface PaymentService {

    /**
     * Pays for a confirmed booking.
     * @return a short receipt text
     * @throws PaymentException if the payment is not allowed
     */
    String pay(Booking booking) throws PaymentException;
}
