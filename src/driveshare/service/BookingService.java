package driveshare.service;

import driveshare.auth.SessionManager;
import driveshare.db.BookingDAO;
import driveshare.db.CarDAO;
import driveshare.db.DataException;
import driveshare.model.Booking;
import driveshare.model.BookingStatus;
import driveshare.model.Car;
import driveshare.model.User;
import driveshare.observer.CarEvent;
import driveshare.observer.CarSubject;

import java.time.LocalDate;
import java.util.List;

/**
 * Booking system: renters request a car for a period, owners confirm or decline,
 * renters can cancel. Both sides get a notification at every step.
 *
 * Double-booking is prevented in two layers:
 *   1. isAvailable() checks for an overlapping active booking before inserting, and
 *   2. the database trigger "prevent_overlapping_bookings" rejects any overlapping insert.
 */
public class BookingService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final CarDAO carDAO = new CarDAO();
    private final NotificationService notifications = new NotificationService();
    private final CarSubject carSubject;

    public BookingService(CarSubject carSubject) {
        this.carSubject = carSubject;
    }

    /** True if the listing is active, covers the dates, and has no overlapping active booking. */
    public boolean isAvailable(Car car, LocalDate start, LocalDate end) {
        return car.isActive() && car.coversDates(start, end) && !bookingDAO.hasOverlap(car.getId(), start, end);
    }

    /** Creates a PENDING booking request and notifies the owner and the renter. */
    public Booking requestBooking(Car car, LocalDate start, LocalDate end) {
        User renter = SessionManager.getInstance().requireUser();
        Car fresh = carDAO.findById(car.getId());

        if (fresh.getOwnerId() == renter.getId())
            throw new IllegalArgumentException("You cannot book your own car.");
        if (start.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("The start date cannot be in the past.");
        if (end.isBefore(start))
            throw new IllegalArgumentException("The end date must be on or after the start date.");
        if (!fresh.isActive())
            throw new IllegalArgumentException("This listing is currently paused by the owner.");
        if (!fresh.coversDates(start, end))
            throw new IllegalArgumentException("The owner only offers this car from " + fresh.getAvailableFrom()
                    + " to " + fresh.getAvailableTo() + ".");
        if (bookingDAO.hasOverlap(fresh.getId(), start, end))
            throw new IllegalArgumentException("Sorry, this car is already booked for some of these dates.");

        double total = Booking.countDays(start, end) * fresh.getPricePerDay();
        int id;
        try {
            id = bookingDAO.insert(fresh.getId(), renter.getId(), start, end, total);
        } catch (DataException e) {
            // the database trigger caught an overlap that happened at the same moment
            throw new IllegalArgumentException("Sorry, this car was just booked for overlapping dates.");
        }
        Booking booking = bookingDAO.findById(id);

        notifications.notifyUser(fresh.getOwnerId(), String.format(
                "New booking request: %s wants your %s from %s to %s ($%.2f). Please confirm or decline.",
                renter.getFullName(), fresh.getTitle(), start, end, total));
        notifications.notifyUser(renter.getId(), String.format(
                "Booking request sent for the %s (%s to %s). Waiting for the owner to confirm.",
                fresh.getTitle(), start, end));
        return booking;
    }

    /** Owner accepts a pending request. */
    public void confirm(Booking booking) {
        Booking b = requireOwnerOf(booking, BookingStatus.PENDING);
        bookingDAO.updateStatus(b.getId(), BookingStatus.CONFIRMED);
        notifications.notifyUser(b.getRenterId(), String.format(
                "Booking confirmed: %s confirmed your %s from %s to %s. Please pay $%.2f to complete it.",
                b.getOwnerName(), b.getCarTitle(), b.getStartDate(), b.getEndDate(), b.getTotalPrice()));
    }

    /** Owner rejects a pending request; the dates become free again. */
    public void decline(Booking booking) {
        Booking b = requireOwnerOf(booking, BookingStatus.PENDING);
        bookingDAO.updateStatus(b.getId(), BookingStatus.DECLINED);
        notifications.notifyUser(b.getRenterId(), String.format(
                "Booking declined: your request for the %s (%s to %s) was declined by the owner.",
                b.getCarTitle(), b.getStartDate(), b.getEndDate()));
        carSubject.notifyObservers(carDAO.findById(b.getCarId()), CarEvent.AVAILABILITY_CHANGED);
    }

    /** Renter cancels a pending or confirmed (unpaid) booking; the dates become free again. */
    public void cancel(Booking booking) {
        User renter = SessionManager.getInstance().requireUser();
        Booking b = bookingDAO.findById(booking.getId());
        if (b == null || b.getRenterId() != renter.getId())
            throw new IllegalArgumentException("You can only cancel your own bookings.");
        if (b.getStatus() != BookingStatus.PENDING && b.getStatus() != BookingStatus.CONFIRMED)
            throw new IllegalArgumentException("Only pending or confirmed (unpaid) bookings can be cancelled.");
        bookingDAO.updateStatus(b.getId(), BookingStatus.CANCELLED);
        notifications.notifyUser(b.getOwnerId(), String.format(
                "Booking cancelled: %s cancelled the booking of your %s (%s to %s).",
                b.getRenterName(), b.getCarTitle(), b.getStartDate(), b.getEndDate()));
        carSubject.notifyObservers(carDAO.findById(b.getCarId()), CarEvent.AVAILABILITY_CHANGED);
    }

    /** Bookings the logged-in user made as a renter. */
    public List<Booking> getMyTrips() {
        return bookingDAO.findByRenter(SessionManager.getInstance().requireUser().getId());
    }

    /** Bookings made on the logged-in user's cars. */
    public List<Booking> getRequestsForMyCars() {
        return bookingDAO.findByOwner(SessionManager.getInstance().requireUser().getId());
    }

    /** Active bookings of a car (for the availability calendar). */
    public List<Booking> getActiveBookings(int carId) {
        return bookingDAO.findActiveByCar(carId);
    }

    private Booking requireOwnerOf(Booking booking, BookingStatus expected) {
        User owner = SessionManager.getInstance().requireUser();
        Booking b = bookingDAO.findById(booking.getId());
        if (b == null || b.getOwnerId() != owner.getId())
            throw new IllegalArgumentException("Only the car owner can do this.");
        if (b.getStatus() != expected)
            throw new IllegalArgumentException("This booking is already " + b.getStatus() + ".");
        return b;
    }
}
