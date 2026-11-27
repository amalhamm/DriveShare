package driveshare.model;

import java.time.LocalDate;

/**
 * BUILDER PATTERN - builds {@link Car} listing objects.
 *
 * A car listing has many attributes and several of them are optional
 * (color, seats, transmission, fuel type, description). Instead of a constructor
 * with 16 parameters, the client sets only what it needs, step by step, and calls build():
 *
 * <pre>
 *   Car car = new CarListingBuilder(ownerId, "Honda Civic", 2019)
 *                 .mileage(42000)
 *                 .pickupLocation("Dearborn, MI")
 *                 .pricePerDay(40)
 *                 .availability(from, to)
 *                 .color("Blue")              // optional
 *                 .build();
 * </pre>
 *
 * build() checks that the required details are valid, so an invalid Car can never exist.
 * The builder is used in two places: the "Add / Edit Car" form (CarFormDialog) and
 * CarDAO when rows are loaded from the database.
 */
public class CarListingBuilder {

    // Fields are package-private so the Car constructor can read them.
    // required
    int id;                 // 0 until the car is saved in the database
    int ownerId;
    String ownerName = "";
    String model;
    int year;
    int mileage = -1;
    String pickupLocation;
    double pricePerDay = -1;
    LocalDate availableFrom;
    LocalDate availableTo;
    boolean active = true;

    // optional, with default values
    String color = "Not specified";
    int seats = 5;
    String transmission = "Automatic";
    String fuelType = "Gasoline";
    String description = "";

    /** The three details every listing must start with. */
    public CarListingBuilder(int ownerId, String model, int year) {
        this.ownerId = ownerId;
        this.model = model;
        this.year = year;
    }

    /** Copies an existing car into a new builder so a listing can be edited. */
    public static CarListingBuilder from(Car car) {
        return new CarListingBuilder(car.getOwnerId(), car.getModel(), car.getYear())
                .id(car.getId())
                .ownerName(car.getOwnerName())
                .mileage(car.getMileage())
                .pickupLocation(car.getPickupLocation())
                .pricePerDay(car.getPricePerDay())
                .availability(car.getAvailableFrom(), car.getAvailableTo())
                .active(car.isActive())
                .color(car.getColor())
                .seats(car.getSeats())
                .transmission(car.getTransmission())
                .fuelType(car.getFuelType())
                .description(car.getDescription());
    }

    // ---- step methods (each returns this so calls can be chained) ----

    public CarListingBuilder id(int id) { this.id = id; return this; }
    public CarListingBuilder ownerName(String ownerName) { this.ownerName = ownerName; return this; }
    public CarListingBuilder model(String model) { this.model = model; return this; }
    public CarListingBuilder year(int year) { this.year = year; return this; }
    public CarListingBuilder mileage(int mileage) { this.mileage = mileage; return this; }
    public CarListingBuilder pickupLocation(String location) { this.pickupLocation = location; return this; }
    public CarListingBuilder pricePerDay(double price) { this.pricePerDay = price; return this; }
    public CarListingBuilder active(boolean active) { this.active = active; return this; }

    public CarListingBuilder availability(LocalDate from, LocalDate to) {
        this.availableFrom = from;
        this.availableTo = to;
        return this;
    }

    public CarListingBuilder color(String color) {
        if (color != null && !color.trim().isEmpty()) this.color = color.trim();
        return this;
    }

    public CarListingBuilder seats(int seats) { this.seats = seats; return this; }

    public CarListingBuilder transmission(String transmission) {
        if (transmission != null && !transmission.trim().isEmpty()) this.transmission = transmission.trim();
        return this;
    }

    public CarListingBuilder fuelType(String fuelType) {
        if (fuelType != null && !fuelType.trim().isEmpty()) this.fuelType = fuelType.trim();
        return this;
    }

    public CarListingBuilder description(String description) {
        this.description = description == null ? "" : description.trim();
        return this;
    }

    /**
     * Validates the required details and creates the Car.
     *
     * @throws IllegalStateException with a user-friendly message if something is missing or invalid
     */
    public Car build() {
        int nextYear = LocalDate.now().getYear() + 1;
        if (model == null || model.trim().isEmpty())
            throw new IllegalStateException("Car model is required.");
        if (year < 1980 || year > nextYear)
            throw new IllegalStateException("Year must be between 1980 and " + nextYear + ".");
        if (mileage < 0)
            throw new IllegalStateException("Mileage must be 0 or more.");
        if (pickupLocation == null || pickupLocation.trim().isEmpty())
            throw new IllegalStateException("Pick-up location is required.");
        if (pricePerDay <= 0)
            throw new IllegalStateException("Price per day must be greater than 0.");
        if (availableFrom == null || availableTo == null)
            throw new IllegalStateException("Availability dates are required.");
        if (availableTo.isBefore(availableFrom))
            throw new IllegalStateException("'Available to' must be on or after 'Available from'.");
        if (seats < 1 || seats > 15)
            throw new IllegalStateException("Seats must be between 1 and 15.");

        model = model.trim();
        pickupLocation = pickupLocation.trim();
        return new Car(this);
    }
}
