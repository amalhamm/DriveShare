package driveshare.service;

import driveshare.auth.SessionManager;
import driveshare.db.CarDAO;
import driveshare.model.Car;
import driveshare.model.CarListingBuilder;
import driveshare.model.User;
import driveshare.observer.CarEvent;
import driveshare.observer.CarSubject;

import java.util.List;

/**
 * Owner-side logic: create and manage car listings.
 * When a price or availability changes, the Observer subject is told, so renters who
 * watch the car can be notified.
 */
public class CarService {

    private final CarDAO carDAO = new CarDAO();
    private final CarSubject carSubject;

    public CarService(CarSubject carSubject) {
        this.carSubject = carSubject;
    }

    /** Saves a new listing (built with CarListingBuilder) for the logged-in owner. */
    public Car createListing(Car car) {
        User owner = SessionManager.getInstance().requireUser();
        if (car.getOwnerId() != owner.getId()) {
            throw new SecurityException("You can only list your own cars.");
        }
        return carDAO.insert(car);
    }

    /**
     * Saves changes to a listing. Compares the old and new values and notifies the
     * watchers if the price or the availability changed.
     */
    public Car updateListing(Car updated) {
        User owner = SessionManager.getInstance().requireUser();
        Car old = carDAO.findById(updated.getId());
        if (old == null || old.getOwnerId() != owner.getId()) {
            throw new SecurityException("You can only edit your own listings.");
        }
        carDAO.update(updated);
        Car saved = carDAO.findById(updated.getId());

        if (Double.compare(old.getPricePerDay(), saved.getPricePerDay()) != 0) {
            carSubject.notifyObservers(saved, CarEvent.PRICE_CHANGED);
        }
        boolean availabilityChanged = old.isActive() != saved.isActive()
                || !old.getAvailableFrom().equals(saved.getAvailableFrom())
                || !old.getAvailableTo().equals(saved.getAvailableTo());
        if (availabilityChanged) {
            carSubject.notifyObservers(saved, CarEvent.AVAILABILITY_CHANGED);
        }
        return saved;
    }

    /** Pauses (hides from search) or resumes a listing. */
    public Car setActive(Car car, boolean active) {
        return updateListing(CarListingBuilder.from(car).active(active).build());
    }

    public List<Car> getMyCars() {
        return carDAO.findByOwner(SessionManager.getInstance().requireUser().getId());
    }

    public Car findById(int carId) {
        return carDAO.findById(carId);
    }

    /** Cars of other owners that match the location, maximum price and minimum seats. */
    public List<Car> search(String location, Double maxPrice, int minSeats) {
        int me = SessionManager.getInstance().requireUser().getId();
        return carDAO.search(me, location == null ? "" : location, maxPrice, minSeats);
    }
}
