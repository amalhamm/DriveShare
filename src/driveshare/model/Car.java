package driveshare.model;

import java.time.LocalDate;

/**
 * A car listing (the "Product" built by the Builder pattern).
 *
 * A Car is immutable: it can only be created through {@link CarListingBuilder}.
 * To change a listing, the code copies it into a builder with
 * {@link CarListingBuilder#from(Car)}, changes the fields it needs and builds a new Car.
 *
 * Required details: owner, model, year, mileage, pickup location, price per day and
 * the availability window (availableFrom - availableTo).
 * Optional details: color, seats, transmission, fuel type and description.
 */
public class Car {

    // ---- required attributes ----
    private final int id;
    private final int ownerId;
    private final String ownerName;
    private final String model;
    private final int year;
    private final int mileage;
    private final String pickupLocation;
    private final double pricePerDay;
    private final LocalDate availableFrom;
    private final LocalDate availableTo;
    private final boolean active;       // false = owner paused the listing

    // ---- optional attributes ----
    private final String color;
    private final int seats;
    private final String transmission;
    private final String fuelType;
    private final String description;

    /** Package-private: only CarListingBuilder.build() can create a Car. */
    Car(CarListingBuilder b) {
        this.id = b.id;
        this.ownerId = b.ownerId;
        this.ownerName = b.ownerName;
        this.model = b.model;
        this.year = b.year;
        this.mileage = b.mileage;
        this.pickupLocation = b.pickupLocation;
        this.pricePerDay = b.pricePerDay;
        this.availableFrom = b.availableFrom;
        this.availableTo = b.availableTo;
        this.active = b.active;
        this.color = b.color;
        this.seats = b.seats;
        this.transmission = b.transmission;
        this.fuelType = b.fuelType;
        this.description = b.description;
    }

    public int getId() { return id; }
    public int getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public int getMileage() { return mileage; }
    public String getPickupLocation() { return pickupLocation; }
    public double getPricePerDay() { return pricePerDay; }
    public LocalDate getAvailableFrom() { return availableFrom; }
    public LocalDate getAvailableTo() { return availableTo; }
    public boolean isActive() { return active; }
    public String getColor() { return color; }
    public int getSeats() { return seats; }
    public String getTransmission() { return transmission; }
    public String getFuelType() { return fuelType; }
    public String getDescription() { return description; }

    /** Short display name, e.g. "2019 Honda Civic". */
    public String getTitle() {
        return year + " " + model;
    }

    /** True if the requested dates fall completely inside the owner's availability window. */
    public boolean coversDates(LocalDate start, LocalDate end) {
        return !start.isBefore(availableFrom) && !end.isAfter(availableTo);
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
