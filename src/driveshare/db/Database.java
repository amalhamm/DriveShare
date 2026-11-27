package driveshare.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections to the SQLite database file (driveshare.db) and creates the
 * schema the first time the program runs.
 *
 * Tables: users, security_questions, cars, bookings, payments, messages, watches, notifications.
 * The full schema is described in docs/Report.docx.
 */
public final class Database {

    /** The database file is created in the folder the program is started from. */
    private static final String URL = "jdbc:sqlite:driveshare.db";

    private Database() { }

    /** Opens a new connection with foreign-key checks turned on. Callers close it (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /** Creates all tables (if they do not exist yet) and loads demo data into an empty database. */
    public static void initialize() {
        String[] schema = {
            // ---------------- users ----------------
            "CREATE TABLE IF NOT EXISTS users ("
                + " id            INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " email         TEXT NOT NULL UNIQUE,"
                + " full_name     TEXT NOT NULL,"
                + " password_hash TEXT NOT NULL,"
                + " password_salt TEXT NOT NULL,"
                + " balance       REAL NOT NULL DEFAULT 0,"
                + " created_at    TEXT NOT NULL)",

            // ---------------- security questions (3 per user) ----------------
            "CREATE TABLE IF NOT EXISTS security_questions ("
                + " id             INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " user_id        INTEGER NOT NULL REFERENCES users(id),"
                + " question_order INTEGER NOT NULL CHECK (question_order BETWEEN 1 AND 3),"
                + " question       TEXT NOT NULL,"
                + " answer_hash    TEXT NOT NULL,"
                + " answer_salt    TEXT NOT NULL,"
                + " UNIQUE (user_id, question_order))",

            // ---------------- cars ----------------
            "CREATE TABLE IF NOT EXISTS cars ("
                + " id              INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " owner_id        INTEGER NOT NULL REFERENCES users(id),"
                + " model           TEXT NOT NULL,"
                + " year            INTEGER NOT NULL,"
                + " mileage         INTEGER NOT NULL,"
                + " pickup_location TEXT NOT NULL,"
                + " price_per_day   REAL NOT NULL CHECK (price_per_day > 0),"
                + " available_from  TEXT NOT NULL,"
                + " available_to    TEXT NOT NULL,"
                + " is_active       INTEGER NOT NULL DEFAULT 1,"
                + " color           TEXT,"
                + " seats           INTEGER,"
                + " transmission    TEXT,"
                + " fuel_type       TEXT,"
                + " description     TEXT)",

            // ---------------- bookings ----------------
            "CREATE TABLE IF NOT EXISTS bookings ("
                + " id          INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " car_id      INTEGER NOT NULL REFERENCES cars(id),"
                + " renter_id   INTEGER NOT NULL REFERENCES users(id),"
                + " start_date  TEXT NOT NULL,"
                + " end_date    TEXT NOT NULL,"
                + " total_price REAL NOT NULL,"
                + " status      TEXT NOT NULL CHECK (status IN ('PENDING','CONFIRMED','PAID','DECLINED','CANCELLED')),"
                + " created_at  TEXT NOT NULL,"
                + " CHECK (end_date >= start_date))",

            // Safety net at the database level: a new active booking can never overlap
            // another active booking of the same car (the app also checks this first).
            "CREATE TRIGGER IF NOT EXISTS prevent_overlapping_bookings"
                + " BEFORE INSERT ON bookings"
                + " WHEN NEW.status IN ('PENDING','CONFIRMED','PAID')"
                + " BEGIN"
                + "   SELECT RAISE(ABORT, 'This car is already booked for overlapping dates.')"
                + "   WHERE EXISTS (SELECT 1 FROM bookings b"
                + "                 WHERE b.car_id = NEW.car_id"
                + "                   AND b.status IN ('PENDING','CONFIRMED','PAID')"
                + "                   AND NEW.start_date <= b.end_date"
                + "                   AND NEW.end_date >= b.start_date);"
                + " END",

            // ---------------- payments (simulated) ----------------
            "CREATE TABLE IF NOT EXISTS payments ("
                + " id         INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " booking_id INTEGER NOT NULL UNIQUE REFERENCES bookings(id),"
                + " payer_id   INTEGER NOT NULL REFERENCES users(id),"
                + " payee_id   INTEGER NOT NULL REFERENCES users(id),"
                + " amount     REAL NOT NULL,"
                + " paid_at    TEXT NOT NULL)",

            // ---------------- messages ----------------
            "CREATE TABLE IF NOT EXISTS messages ("
                + " id          INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " sender_id   INTEGER NOT NULL REFERENCES users(id),"
                + " receiver_id INTEGER NOT NULL REFERENCES users(id),"
                + " body        TEXT NOT NULL,"
                + " sent_at     TEXT NOT NULL)",

            // ---------------- watches (renter watch list) ----------------
            "CREATE TABLE IF NOT EXISTS watches ("
                + " id                    INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " renter_id             INTEGER NOT NULL REFERENCES users(id),"
                + " car_id                INTEGER NOT NULL REFERENCES cars(id),"
                + " max_price             REAL,"
                + " notify_when_available INTEGER NOT NULL DEFAULT 0,"
                + " created_at            TEXT NOT NULL,"
                + " UNIQUE (renter_id, car_id))",

            // ---------------- notifications ----------------
            "CREATE TABLE IF NOT EXISTS notifications ("
                + " id         INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " user_id    INTEGER NOT NULL REFERENCES users(id),"
                + " message    TEXT NOT NULL,"
                + " created_at TEXT NOT NULL,"
                + " is_read    INTEGER NOT NULL DEFAULT 0)"
        };

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String sql : schema) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new DataException("Could not create the database: " + e.getMessage(), e);
        }

        DemoData.seedIfEmpty();
    }
}
