package driveshare.model;

/**
 * A renter's "watch" on a car, with the renter's own conditions:
 *  - maxPrice: notify when the price per day drops to this value or lower (null = not used)
 *  - notifyWhenAvailable: notify when the car becomes available again
 *
 * Each Watch is wrapped by a WatchObserver (Observer pattern) at run time.
 */
public class Watch {

    private final int id;
    private final int renterId;
    private final int carId;
    private final String carTitle;
    private final Double maxPrice;
    private final boolean notifyWhenAvailable;

    public Watch(int id, int renterId, int carId, String carTitle, Double maxPrice, boolean notifyWhenAvailable) {
        this.id = id;
        this.renterId = renterId;
        this.carId = carId;
        this.carTitle = carTitle;
        this.maxPrice = maxPrice;
        this.notifyWhenAvailable = notifyWhenAvailable;
    }

    public int getId() { return id; }
    public int getRenterId() { return renterId; }
    public int getCarId() { return carId; }
    public String getCarTitle() { return carTitle; }
    public Double getMaxPrice() { return maxPrice; }
    public boolean isNotifyWhenAvailable() { return notifyWhenAvailable; }
}
