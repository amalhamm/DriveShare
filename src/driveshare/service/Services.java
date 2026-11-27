package driveshare.service;

import driveshare.auth.AuthService;
import driveshare.observer.CarWatchNotifier;
import driveshare.payment.PaymentProxy;
import driveshare.payment.PaymentService;
import driveshare.recovery.PasswordRecoveryService;

/**
 * Creates the application's services once and wires them together, then gives them to the screens.
 * The Observer subject (CarWatchNotifier) is shared by CarService, BookingService and WatchService.
 * The screens get the PaymentProxy, never the real payment service.
 */
public class Services {

    public final CarWatchNotifier carWatchNotifier = new CarWatchNotifier();
    public final AuthService auth = new AuthService();
    public final PasswordRecoveryService recovery = new PasswordRecoveryService();
    public final CarService cars = new CarService(carWatchNotifier);
    public final BookingService bookings = new BookingService(carWatchNotifier);
    public final WatchService watches = new WatchService(carWatchNotifier);
    public final MessageService messages = new MessageService();
    public final NotificationService notifications = new NotificationService();
    public final PaymentService payment = new PaymentProxy();

    public Services() {
        carWatchNotifier.loadFromDatabase();   // re-attach observers for saved watches
    }
}
