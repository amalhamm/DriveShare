package driveshare.service;

import driveshare.auth.SessionManager;
import driveshare.db.WatchDAO;
import driveshare.model.Car;
import driveshare.model.User;
import driveshare.model.Watch;
import driveshare.observer.CarWatchNotifier;
import driveshare.observer.WatchObserver;

import java.util.List;

/**
 * Renter watch list. Saving a watch also attaches a WatchObserver to the subject,
 * and removing it detaches the observer.
 */
public class WatchService {

    private final WatchDAO watchDAO = new WatchDAO();
    private final CarWatchNotifier notifier;

    public WatchService(CarWatchNotifier notifier) {
        this.notifier = notifier;
    }

    /**
     * Watches a car with the renter's conditions.
     * @param maxPrice notify when the price drops to this or lower (null = do not watch the price)
     */
    public Watch watch(Car car, Double maxPrice, boolean notifyWhenAvailable) {
        User renter = SessionManager.getInstance().requireUser();
        if (car.getOwnerId() == renter.getId())
            throw new IllegalArgumentException("You cannot watch your own car.");
        if (maxPrice == null && !notifyWhenAvailable)
            throw new IllegalArgumentException("Choose at least one condition: a maximum price or availability.");
        if (maxPrice != null && maxPrice <= 0)
            throw new IllegalArgumentException("Maximum price must be greater than 0.");

        Watch saved = watchDAO.upsert(renter.getId(), car.getId(), maxPrice, notifyWhenAvailable);
        notifier.detachWatch(saved.getId());          // replace the old observer if the watch was edited
        notifier.attach(new WatchObserver(saved));    // register the observer with the subject
        return saved;
    }

    public void remove(Watch watch) {
        watchDAO.delete(watch.getId());
        notifier.detachWatch(watch.getId());
    }

    public List<Watch> getMyWatches() {
        return watchDAO.findByRenter(SessionManager.getInstance().requireUser().getId());
    }
}
