package driveshare.db;

import driveshare.auth.AuthService;
import driveshare.model.CarListingBuilder;
import driveshare.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Fills an empty database with two demo accounts and a few cars so the program can be
 * demonstrated right away. Delete driveshare.db to start over with fresh demo data.
 *
 *   Owner  : sara@driveshare.com / owner123
 *   Renter : adam@driveshare.com / renter123
 *   Security answers for both: Dearborn / Max / Pizza
 */
final class DemoData {

    private DemoData() { }

    static void seedIfEmpty() {
        try (Connection conn = Database.getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        } catch (SQLException e) {
            throw new DataException("Could not check demo data: " + e.getMessage(), e);
        }

        String[] questions = { AuthService.QUESTION_CHOICES[0], AuthService.QUESTION_CHOICES[1],
                               AuthService.QUESTION_CHOICES[2] };
        String[] answers = { "Dearborn", "Max", "Pizza" };

        AuthService auth = new AuthService();
        User sara = auth.register("sara@driveshare.com", "Sara Haddad", "owner123", "owner123", questions, answers);
        auth.register("adam@driveshare.com", "Adam Saleh", "renter123", "renter123", questions, answers);

        LocalDate today = LocalDate.now();
        CarDAO cars = new CarDAO();
        cars.insert(new CarListingBuilder(sara.getId(), "Honda Civic", 2019)
                .mileage(42_000).pickupLocation("Dearborn, MI").pricePerDay(45)
                .availability(today, today.plusDays(90))
                .color("Blue").seats(5).transmission("Automatic").fuelType("Gasoline")
                .description("Clean, great on gas. Phone mount included.")
                .build());
        cars.insert(new CarListingBuilder(sara.getId(), "Tesla Model 3", 2022)
                .mileage(18_500).pickupLocation("Ann Arbor, MI").pricePerDay(95)
                .availability(today, today.plusDays(60))
                .color("White").seats(5).fuelType("Electric")
                .description("Long range. Charging cable included.")
                .build());
        cars.insert(new CarListingBuilder(sara.getId(), "Jeep Wrangler", 2020)
                .mileage(36_000).pickupLocation("Detroit, MI").pricePerDay(80)
                .availability(today.plusDays(3), today.plusDays(45))
                .color("Red").seats(4).transmission("Manual")
                .build());
        cars.insert(new CarListingBuilder(sara.getId(), "Toyota Camry", 2018)
                .mileage(61_000).pickupLocation("Dearborn, MI").pricePerDay(40)
                .availability(today, today.plusDays(120))
                .build());   // only required details - optional ones keep their defaults

        System.out.println("[DemoData] Created demo users and cars");
    }
}
