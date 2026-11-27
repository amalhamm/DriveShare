package driveshare.payment;

/** Thrown when a payment is refused (not logged in, wrong user, not enough balance, ...). */
public class PaymentException extends Exception {
    public PaymentException(String message) {
        super(message);
    }
}
