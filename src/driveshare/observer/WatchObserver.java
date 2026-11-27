package driveshare.observer;

import driveshare.model.Car;
import driveshare.model.Watch;
import driveshare.service.NotificationService;

/**
 * OBSERVER PATTERN - Concrete Observer.
 *
 * One WatchObserver exists for each renter's watch. When the subject calls update(),
 * the observer checks the renter's own conditions and sends the renter an in-app
 * notification only if they are met:
 *   - PRICE_CHANGED: new price per day <= the renter's maximum price
 *   - AVAILABILITY_CHANGED: the renter asked to be told when the car is available,
 *     and the listing is active
 */
public class WatchObserver implements CarObserver {

    private final Watch watch;
    private final NotificationService notificationService = new NotificationService();

    public WatchObserver(Watch watch) {
        this.watch = watch;
    }

    @Override
    public void update(Car car, CarEvent event) {
        if (car.getOwnerId() == watch.getRenterId()) return;   // never notify an owner about their own car

        if (event == CarEvent.PRICE_CHANGED && watch.getMaxPrice() != null
                && car.isActive() && car.getPricePerDay() <= watch.getMaxPrice()) {
            notificationService.notifyUser(watch.getRenterId(), String.format(
                    "Price alert: the %s you are watching is now $%.2f/day (your limit: $%.2f).",
                    car.getTitle(), car.getPricePerDay(), watch.getMaxPrice()));
        }

        if (event == CarEvent.AVAILABILITY_CHANGED && watch.isNotifyWhenAvailable() && car.isActive()) {
            notificationService.notifyUser(watch.getRenterId(), String.format(
                    "Availability alert: the %s you are watching is available from %s to %s in %s ($%.2f/day).",
                    car.getTitle(), car.getAvailableFrom(), car.getAvailableTo(),
                    car.getPickupLocation(), car.getPricePerDay()));
        }
    }

    @Override
    public int getCarId() {
        return watch.getCarId();
    }

    public int getWatchId() {
        return watch.getId();
    }
}
