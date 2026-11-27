package driveshare.model;

/**
 * Life cycle of a booking:
 *
 *   PENDING --(owner confirms)--> CONFIRMED --(renter pays)--> PAID
 *      |                              |
 *      +--(owner declines)--> DECLINED +--(renter cancels)--> CANCELLED
 *      +--(renter cancels)--> CANCELLED
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    PAID,
    DECLINED,
    CANCELLED;

    /** PENDING, CONFIRMED and PAID bookings hold the car, so no other booking may overlap them. */
    public boolean blocksCar() {
        return this == PENDING || this == CONFIRMED || this == PAID;
    }
}
