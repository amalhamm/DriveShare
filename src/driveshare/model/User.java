package driveshare.model;

/**
 * A DriveShare user. Every user can act as both a car owner (listing cars)
 * and a renter (booking other people's cars), like on Turo.
 *
 * Password and security-answer hashes are NOT stored in this object; they stay
 * in the database and are only read by the authentication / recovery code.
 */
public class User {

    private final int id;
    private final String email;
    private final String fullName;
    private double balance; // simulated wallet balance used by the payment feature

    public User(int id, String email, String fullName, double balance) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.balance = balance;
    }

    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public double getBalance() { return balance; }

    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String toString() {
        return fullName + " (" + email + ")";
    }
}
