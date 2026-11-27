package driveshare.observer;

import driveshare.db.WatchDAO;
import driveshare.model.Car;
import driveshare.model.Watch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OBSERVER PATTERN - Concrete Subject.
 *
 * Keeps, for every car id, the list of observers (renters' watches) of that car.
 * CarService calls notifyObservers(...) whenever an owner changes a car's price or
 * availability, and BookingService calls it when a booking is declined or cancelled
 * (the car becomes free again). The subject does not know what the observers do; it only
 * calls update() on each of them.
 *
 * Watches are saved in the database, so at start-up loadFromDatabase() re-attaches an
 * observer for every saved watch.
 */
public class CarWatchNotifier implements CarSubject {

    private final Map<Integer, List<CarObserver>> observersByCar = new HashMap<>();

    /** Re-creates the observers for all watches stored in the database. */
    public void loadFromDatabase() {
        observersByCar.clear();
        for (Watch watch : new WatchDAO().findAll()) {
            attach(new WatchObserver(watch));
        }
    }

    @Override
    public void attach(CarObserver observer) {
        observersByCar.computeIfAbsent(observer.getCarId(), id -> new ArrayList<>()).add(observer);
    }

    @Override
    public void detach(CarObserver observer) {
        List<CarObserver> list = observersByCar.get(observer.getCarId());
        if (list != null) list.remove(observer);
    }

    /** Removes the observer that wraps a given watch (when a renter removes it from the watch list). */
    public void detachWatch(int watchId) {
        for (List<CarObserver> list : observersByCar.values()) {
            list.removeIf(o -> o instanceof WatchObserver && ((WatchObserver) o).getWatchId() == watchId);
        }
    }

    @Override
    public void notifyObservers(Car car, CarEvent event) {
        List<CarObserver> list = observersByCar.get(car.getId());
        if (list == null) return;
        System.out.println("[Observer] " + car.getTitle() + " " + event + " -> notifying " + list.size() + " watcher(s)");
        for (CarObserver observer : new ArrayList<>(list)) {
            observer.update(car, event);
        }
    }
}
