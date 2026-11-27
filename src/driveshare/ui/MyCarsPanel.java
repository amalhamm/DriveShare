package driveshare.ui;

import driveshare.auth.SessionManager;
import driveshare.model.Car;
import driveshare.model.CarListingBuilder;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** Owner screen: list cars and manage them (edit, update price, update availability, pause/resume). */
public class MyCarsPanel extends UIComponent {

    private final DefaultTableModel model = UIUtil.readOnlyModel(
            "ID", "Car", "Pick-up location", "Price / day", "Mileage", "Available from", "Available to",
            "Status", "Optional details");
    private final JTable table = UIUtil.table(model);
    private final List<Car> cars = new ArrayList<>();

    public MyCarsPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new BorderLayout(0, 10));
        UIUtil.pad(this, 14);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIUtil.title("My car listings"), BorderLayout.NORTH);
        top.add(UIUtil.muted("List your car, then manage its price and availability. "
                + "Renters watching a car are notified when these change."), BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(UIUtil.scroll(table), BorderLayout.CENTER);

        JButton addButton = UIUtil.primaryButton("List a New Car");
        JButton editButton = UIUtil.button("Edit Listing");
        JButton priceButton = UIUtil.button("Update Price");
        JButton availabilityButton = UIUtil.button("Update Availability");
        JButton pauseButton = UIUtil.button("Pause / Resume");
        JButton calendarButton = UIUtil.button("View Calendar");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(calendarButton);
        actions.add(pauseButton);
        actions.add(availabilityButton);
        actions.add(priceButton);
        actions.add(editButton);
        actions.add(addButton);
        add(actions, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addCar());
        editButton.addActionListener(e -> editCar());
        priceButton.addActionListener(e -> updatePrice());
        availabilityButton.addActionListener(e -> updateAvailability());
        pauseButton.addActionListener(e -> togglePause());
        calendarButton.addActionListener(e -> {
            Car car = selectedCar();
            if (car != null) {
                new CalendarDialog(SwingUtilities.getWindowAncestor(this), car,
                        services.bookings.getActiveBookings(car.getId())).setVisible(true);
            }
        });
    }

    @Override
    public void refresh() {
        if (!SessionManager.getInstance().isLoggedIn()) return;
        model.setRowCount(0);
        cars.clear();
        cars.addAll(services.cars.getMyCars());
        for (Car car : cars) {
            model.addRow(new Object[] {
                car.getId(), car.getTitle(), car.getPickupLocation(), UIUtil.money(car.getPricePerDay()),
                String.format("%,d mi", car.getMileage()), car.getAvailableFrom(), car.getAvailableTo(),
                car.isActive() ? "Active" : "Paused",
                car.getColor() + ", " + car.getSeats() + " seats, " + car.getTransmission() + ", " + car.getFuelType()
            });
        }
    }

    private Car selectedCar() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.error(this, "Please select one of your cars first.");
            return null;
        }
        return cars.get(table.convertRowIndexToModel(row));
    }

    private void addCar() {
        CarFormDialog dialog = new CarFormDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);
        Car car = dialog.getResult();
        if (car == null) return;
        services.cars.createListing(car);
        mediator.notify(this, UIEvent.LISTING_SAVED, null);
    }

    private void editCar() {
        Car car = selectedCar();
        if (car == null) return;
        CarFormDialog dialog = new CarFormDialog(SwingUtilities.getWindowAncestor(this), car);
        dialog.setVisible(true);
        if (dialog.getResult() != null) save(dialog.getResult());
    }

    private void updatePrice() {
        Car car = selectedCar();
        if (car == null) return;
        String input = JOptionPane.showInputDialog(this, "New price per day for the " + car.getTitle()
                + " (now " + UIUtil.money(car.getPricePerDay()) + "):", String.valueOf(car.getPricePerDay()));
        if (input == null) return;
        try {
            Double price = UIUtil.parseOptionalMoney(input);
            if (price == null) throw new IllegalArgumentException("Please enter a price.");
            save(CarListingBuilder.from(car).pricePerDay(price).build());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    private void updateAvailability() {
        Car car = selectedCar();
        if (car == null) return;
        JSpinner from = UIUtil.dateSpinner(car.getAvailableFrom());
        JSpinner to = UIUtil.dateSpinner(car.getAvailableTo());
        JCheckBox active = new JCheckBox("Listing is active (shown in search)", car.isActive());
        JPanel form = new JPanel(new GridLayout(5, 1, 4, 4));
        form.add(new JLabel("Available from:"));
        form.add(from);
        form.add(new JLabel("Available to:"));
        form.add(to);
        form.add(active);
        if (JOptionPane.showConfirmDialog(this, form, "Update Availability - " + car.getTitle(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            save(CarListingBuilder.from(car)
                    .availability(UIUtil.dateOf(from), UIUtil.dateOf(to))
                    .active(active.isSelected())
                    .build());
        } catch (IllegalStateException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    private void togglePause() {
        Car car = selectedCar();
        if (car == null) return;
        services.cars.setActive(car, !car.isActive());
        mediator.notify(this, UIEvent.LISTING_SAVED, null);
    }

    private void save(Car updated) {
        try {
            services.cars.updateListing(updated);
            mediator.notify(this, UIEvent.LISTING_SAVED, null);
        } catch (SecurityException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }
}
