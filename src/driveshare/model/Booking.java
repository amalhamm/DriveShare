package driveshare.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A renter's reservation of a car for a period of whole days.
 * Start and end dates are inclusive (start = pick-up day, end = return day).
 *
 * The car title, renter name and owner name are filled in by BookingDAO (SQL join)
 * so the screens can show them without extra queries.
 */
public class Booking {

    private final int id;
    private final int carId;
    private final int renterId;
    private final int ownerId;
    private final String carTitle;
    private final String renterName;
    private final String ownerName;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double totalPrice;
    private final BookingStatus status;

    public Booking(int id, int carId, int renterId, int ownerId, String carTitle, String renterName,
                   String ownerName, LocalDate startDate, LocalDate endDate, double totalPrice,
                   BookingStatus status) {
        this.id = id;
        this.carId = carId;
        this.renterId = renterId;
        this.ownerId = ownerId;
        this.carTitle = carTitle;
        this.renterName = renterName;
        this.ownerName = ownerName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    /** Number of rental days, counting both the start and end day. */
    public static long countDays(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    public int getId() { return id; }
    public int getCarId() { return carId; }
    public int getRenterId() { return renterId; }
    public int getOwnerId() { return ownerId; }
    public String getCarTitle() { return carTitle; }
    public String getRenterName() { return renterName; }
    public String getOwnerName() { return ownerName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus() { return status; }
    public long getDays() { return countDays(startDate, endDate); }
}
