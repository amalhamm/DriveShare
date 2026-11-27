package driveshare.db;

/** Unchecked wrapper for SQL errors so the UI code does not need try/catch for SQLException everywhere. */
public class DataException extends RuntimeException {
    public DataException(String message, Throwable cause) {
        super(message, cause);
    }
}
