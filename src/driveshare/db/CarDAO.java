package driveshare.db;

import driveshare.model.Car;
import driveshare.model.CarListingBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Data access for the cars table. Rows are turned into Car objects with the Builder. */
public class CarDAO {

    private static final String SELECT =
            "SELECT c.*, u.full_name AS owner_name FROM cars c JOIN users u ON u.id = c.owner_id ";

    /** Saves a new listing and returns it with its database id. */
    public Car insert(Car car) {
        String sql = "INSERT INTO cars (owner_id, model, year, mileage, pickup_location, price_per_day,"
                + " available_from, available_to, is_active, color, seats, transmission, fuel_type, description)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(ps, car);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DataException("Could not save car: " + e.getMessage(), e);
        }
    }

    /** Overwrites every column of an existing listing. */
    public void update(Car car) {
        String sql = "UPDATE cars SET owner_id = ?, model = ?, year = ?, mileage = ?, pickup_location = ?,"
                + " price_per_day = ?, available_from = ?, available_to = ?, is_active = ?, color = ?, seats = ?,"
                + " transmission = ?, fuel_type = ?, description = ? WHERE id = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            fill(ps, car);
            ps.setInt(15, car.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataException("Could not update car: " + e.getMessage(), e);
        }
    }

    public Car findById(int id) {
        List<Car> list = query(SELECT + "WHERE c.id = ?", id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Car> findByOwner(int ownerId) {
        return query(SELECT + "WHERE c.owner_id = ? ORDER BY c.id", ownerId);
    }

    /**
     * Search used by renters: active cars of OTHER owners whose pickup location contains the
     * search text, with an optional maximum price and minimum number of seats.
     * Date availability is checked afterwards by BookingService.isAvailable(...).
     */
    public List<Car> search(int excludeOwnerId, String location, Double maxPrice, int minSeats) {
        String sql = SELECT + "WHERE c.is_active = 1 AND c.owner_id <> ?"
                + " AND lower(c.pickup_location) LIKE lower(?)"
                + " AND (? IS NULL OR c.price_per_day <= ?)"
                + " AND c.seats >= ? ORDER BY c.price_per_day";
        return query(sql, excludeOwnerId, "%" + location.trim() + "%", maxPrice, maxPrice, minSeats);
    }

    // ---------------------------------------------------------------- helpers

    private void fill(PreparedStatement ps, Car car) throws SQLException {
        ps.setInt(1, car.getOwnerId());
        ps.setString(2, car.getModel());
        ps.setInt(3, car.getYear());
        ps.setInt(4, car.getMileage());
        ps.setString(5, car.getPickupLocation());
        ps.setDouble(6, car.getPricePerDay());
        ps.setString(7, car.getAvailableFrom().toString());
        ps.setString(8, car.getAvailableTo().toString());
        ps.setInt(9, car.isActive() ? 1 : 0);
        ps.setString(10, car.getColor());
        ps.setInt(11, car.getSeats());
        ps.setString(12, car.getTransmission());
        ps.setString(13, car.getFuelType());
        ps.setString(14, car.getDescription());
    }

    private List<Car> query(String sql, Object... params) {
        List<Car> cars = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cars.add(toCar(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataException("Could not read cars: " + e.getMessage(), e);
        }
        return cars;
    }

    /** Uses the Builder to turn a database row into a Car object. */
    private Car toCar(ResultSet rs) throws SQLException {
        return new CarListingBuilder(rs.getInt("owner_id"), rs.getString("model"), rs.getInt("year"))
                .id(rs.getInt("id"))
                .ownerName(rs.getString("owner_name"))
                .mileage(rs.getInt("mileage"))
                .pickupLocation(rs.getString("pickup_location"))
                .pricePerDay(rs.getDouble("price_per_day"))
                .availability(LocalDate.parse(rs.getString("available_from")),
                              LocalDate.parse(rs.getString("available_to")))
                .active(rs.getInt("is_active") == 1)
                .color(rs.getString("color"))
                .seats(rs.getInt("seats"))
                .transmission(rs.getString("transmission"))
                .fuelType(rs.getString("fuel_type"))
                .description(rs.getString("description"))
                .build();
    }
}
