package driveshare.observer;

/** What changed on a car listing when the subject notifies its observers. */
public enum CarEvent {
    /** The owner changed the price per day. */
    PRICE_CHANGED,
    /** The car became available (listing resumed, dates extended, or a booking was declined / cancelled). */
    AVAILABILITY_CHANGED
}
