package driveshare.ui;

import driveshare.model.Booking;
import driveshare.model.Car;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Renter screen: search cars by location, dates, maximum price and minimum seats.
 * From here the renter can book a car, watch it, see its calendar or message the owner.
 */
public class SearchPanel extends UIComponent {

    private final JTextField locationField = new JTextField(14);
    private final JSpinner fromDate = UIUtil.dateSpinner(LocalDate.now().plusDays(1));
    private final JSpinner toDate = UIUtil.dateSpinner(LocalDate.now().plusDays(3));
    private final JTextField maxPriceField = new JTextField(6);
    private final JSpinner minSeats = new JSpinner(new SpinnerNumberModel(1, 1, 15, 1));
    private final JLabel resultLabel = UIUtil.muted(" ");

    private final DefaultTableModel model = UIUtil.readOnlyModel(
            "Car", "Owner", "Pick-up location", "Price / day", "Seats", "Details", "Mileage",
            "Available for your dates", "Trip total");
    private final JTable table = UIUtil.table(model);
    private final List<Car> results = new ArrayList<>();

    public SearchPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new BorderLayout(0, 10));
        UIUtil.pad(this, 14);

        // ---- search filters ----
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filters.add(new JLabel("Location"));
        filters.add(locationField);
        filters.add(new JLabel("From"));
        filters.add(fromDate);
        filters.add(new JLabel("To"));
        filters.add(toDate);
        filters.add(new JLabel("Max $/day"));
        filters.add(maxPriceField);
        filters.add(new JLabel("Min seats"));
        filters.add(minSeats);
        JButton searchButton = UIUtil.primaryButton("Search");
        filters.add(searchButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIUtil.title("Find a car"), BorderLayout.NORTH);
        top.add(filters, BorderLayout.CENTER);
        top.add(resultLabel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        int[] widths = { 150, 100, 120, 75, 45, 190, 80, 140, 130 };
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        add(UIUtil.scroll(table), BorderLayout.CENTER);

        // ---- actions on the selected car ----
        JButton bookButton = UIUtil.primaryButton("Book Selected Car");
        JButton watchButton = UIUtil.button("Watch Car");
        JButton calendarButton = UIUtil.button("View Calendar");
        JButton messageButton = UIUtil.button("Message Owner");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(calendarButton);
        actions.add(messageButton);
        actions.add(watchButton);
        actions.add(bookButton);
        add(actions, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> search(true));
        locationField.addActionListener(e -> search(true));
        bookButton.addActionListener(e -> book());
        watchButton.addActionListener(e -> watch());
        calendarButton.addActionListener(e -> showCalendar());
        messageButton.addActionListener(e -> {
            Car car = selectedCar();
            if (car != null) mediator.notify(this, UIEvent.OPEN_CHAT, car.getOwnerId());
        });
    }

    @Override
    public void refresh() {
        if (!driveshare.auth.SessionManager.getInstance().isLoggedIn()) return;
        search(false);
    }

    /** Runs the search and fills the table. Cars not free for the dates are still listed so they can be watched. */
    private void search(boolean showErrors) {
        model.setRowCount(0);
        results.clear();
        try {
            LocalDate start = UIUtil.dateOf(fromDate);
            LocalDate end = UIUtil.dateOf(toDate);
            if (end.isBefore(start)) throw new IllegalArgumentException("'To' date must be on or after 'From' date.");
            Double maxPrice = UIUtil.parseOptionalMoney(maxPriceField.getText());
            long days = Booking.countDays(start, end);

            int available = 0;
            for (Car car : services.cars.search(locationField.getText(), maxPrice, (Integer) minSeats.getValue())) {
                boolean free = services.bookings.isAvailable(car, start, end);
                if (free) available++;
                results.add(car);
                model.addRow(new Object[] {
                    car.getTitle(), car.getOwnerName(), car.getPickupLocation(),
                    UIUtil.money(car.getPricePerDay()), car.getSeats(),
                    car.getColor() + ", " + car.getTransmission() + ", " + car.getFuelType(),
                    String.format("%,d mi", car.getMileage()),
                    free ? "Yes" : "No",
                    free ? UIUtil.money(days * car.getPricePerDay()) + " (" + days + " days)" : "-"
                });
            }
            resultLabel.setText(results.size() + " car(s) found, " + available + " available for "
                    + start + " to " + end + ". Unavailable cars can still be watched.");
        } catch (IllegalArgumentException ex) {
            if (showErrors) UIUtil.error(this, ex.getMessage());
        }
    }

    private Car selectedCar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.error(this, "Please select a car first.");
            return null;
        }
        return results.get(table.convertRowIndexToModel(row));
    }

    private void book() {
        Car car = selectedCar();
        if (car == null) return;
        LocalDate start = UIUtil.dateOf(fromDate);
        LocalDate end = UIUtil.dateOf(toDate);
        long days = Booking.countDays(start, end);
        if (!UIUtil.confirm(this, String.format("Request the %s from %s to %s (%d days) for %s?",
                car.getTitle(), start, end, days, UIUtil.money(days * car.getPricePerDay())))) {
            return;
        }
        try {
            services.bookings.requestBooking(car, start, end);
            UIUtil.info(this, "Booking request sent. The owner will confirm it, then you can pay on the Bookings tab.");
            mediator.notify(this, UIEvent.BOOKING_CHANGED, null);
        } catch (IllegalArgumentException | SecurityException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    /** Lets the renter set their own conditions for the watch. */
    private void watch() {
        Car car = selectedCar();
        if (car == null) return;

        JCheckBox priceBox = new JCheckBox("Notify me when the price drops to or below:", true);
        JTextField priceField = new JTextField(String.valueOf((int) Math.max(1, car.getPricePerDay() - 5)), 6);
        JCheckBox availableBox = new JCheckBox("Notify me when this car becomes available", true);

        JPanel pricePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pricePanel.add(priceBox);
        pricePanel.add(new JLabel("$"));
        pricePanel.add(priceField);
        pricePanel.add(new JLabel("/ day"));

        JPanel form = new JPanel(new GridLayout(3, 1, 4, 6));
        form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        form.add(new JLabel("Watch the " + car.getTitle() + " (now " + UIUtil.money(car.getPricePerDay()) + "/day)"));
        form.add(pricePanel);
        form.add(availableBox);

        if (JOptionPane.showConfirmDialog(this, form, "Watch Car", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            Double max = priceBox.isSelected() ? UIUtil.parseOptionalMoney(priceField.getText()) : null;
            if (priceBox.isSelected() && max == null) throw new IllegalArgumentException("Please enter a maximum price.");
            services.watches.watch(car, max, availableBox.isSelected());
            UIUtil.info(this, "You are now watching the " + car.getTitle() + ".");
            mediator.notify(this, UIEvent.WATCH_CHANGED, null);
        } catch (IllegalArgumentException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    private void showCalendar() {
        Car car = selectedCar();
        if (car == null) return;
        new CalendarDialog(SwingUtilities.getWindowAncestor(this), car,
                services.bookings.getActiveBookings(car.getId())).setVisible(true);
    }
}
