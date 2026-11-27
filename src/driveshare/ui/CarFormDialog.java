package driveshare.ui;

import driveshare.auth.SessionManager;
import driveshare.model.Car;
import driveshare.model.CarListingBuilder;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;

/**
 * Form to list a new car or edit an existing one.
 * The car is created with the Builder: required details first, then each optional detail
 * the owner filled in, then build() validates everything.
 */
public class CarFormDialog extends JDialog {

    private final Car existing;   // null when listing a new car
    private Car result;

    // required
    private final JTextField modelField = new JTextField(20);
    private final JTextField yearField = new JTextField(6);
    private final JTextField mileageField = new JTextField(8);
    private final JTextField locationField = new JTextField(20);
    private final JTextField priceField = new JTextField(8);
    private final JSpinner fromDate;
    private final JSpinner toDate;
    private final JCheckBox activeBox = new JCheckBox("Listing is active", true);
    // optional
    private final JTextField colorField = new JTextField(12);
    private final JSpinner seatsSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 15, 1));
    private final JComboBox<String> transmissionBox = new JComboBox<>(new String[] { "Automatic", "Manual" });
    private final JComboBox<String> fuelBox =
            new JComboBox<>(new String[] { "Gasoline", "Hybrid", "Electric", "Diesel" });
    private final JTextField descriptionField = new JTextField(24);

    public CarFormDialog(Window owner, Car existing) {
        super(owner, existing == null ? "List a New Car" : "Edit Listing", ModalityType.APPLICATION_MODAL);
        this.existing = existing;

        fromDate = UIUtil.dateSpinner(existing == null ? LocalDate.now() : existing.getAvailableFrom());
        toDate = UIUtil.dateSpinner(existing == null ? LocalDate.now().plusDays(30) : existing.getAvailableTo());
        if (existing != null) fillFrom(existing);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = -1;

        section(form, c, "Required details");
        row(form, c, "Model (make and model)", modelField);
        row(form, c, "Year", yearField);
        row(form, c, "Mileage", mileageField);
        row(form, c, "Pick-up location", locationField);
        row(form, c, "Price per day ($)", priceField);
        row(form, c, "Available from", fromDate);
        row(form, c, "Available to", toDate);
        row(form, c, "", activeBox);
        section(form, c, "Optional details");
        row(form, c, "Color", colorField);
        row(form, c, "Seats", seatsSpinner);
        row(form, c, "Transmission", transmissionBox);
        row(form, c, "Fuel type", fuelBox);
        row(form, c, "Description", descriptionField);

        JButton save = UIUtil.primaryButton(existing == null ? "List Car" : "Save Changes");
        JButton cancel = UIUtil.button("Cancel");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        save.addActionListener(e -> save());
        cancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(save);

        pack();
        setLocationRelativeTo(owner);
    }

    private void fillFrom(Car car) {
        modelField.setText(car.getModel());
        yearField.setText(String.valueOf(car.getYear()));
        mileageField.setText(String.valueOf(car.getMileage()));
        locationField.setText(car.getPickupLocation());
        priceField.setText(String.valueOf(car.getPricePerDay()));
        activeBox.setSelected(car.isActive());
        colorField.setText("Not specified".equals(car.getColor()) ? "" : car.getColor());
        seatsSpinner.setValue(car.getSeats());
        transmissionBox.setSelectedItem(car.getTransmission());
        fuelBox.setSelectedItem(car.getFuelType());
        descriptionField.setText(car.getDescription());
    }

    /** Builds the Car with the Builder; build() reports any invalid detail. */
    private void save() {
        try {
            int ownerId = SessionManager.getInstance().requireUser().getId();
            CarListingBuilder builder = new CarListingBuilder(ownerId, modelField.getText(),
                    parseInt(yearField.getText(), "Year"));
            if (existing != null) builder.id(existing.getId());

            result = builder
                    .mileage(parseInt(mileageField.getText(), "Mileage"))
                    .pickupLocation(locationField.getText())
                    .pricePerDay(parseDouble(priceField.getText(), "Price per day"))
                    .availability(UIUtil.dateOf(fromDate), UIUtil.dateOf(toDate))
                    .active(activeBox.isSelected())
                    .color(colorField.getText())                            // optional
                    .seats((Integer) seatsSpinner.getValue())               // optional
                    .transmission((String) transmissionBox.getSelectedItem())
                    .fuelType((String) fuelBox.getSelectedItem())
                    .description(descriptionField.getText())
                    .build();
            dispose();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            result = null;
            UIUtil.error(this, ex.getMessage());
        }
    }

    /** @return the built car, or null if the dialog was cancelled */
    public Car getResult() {
        return result;
    }

    private static int parseInt(String text, String field) {
        try {
            return Integer.parseInt(text.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a whole number.");
        }
    }

    private static double parseDouble(String text, String field) {
        try {
            return Double.parseDouble(text.trim().replace("$", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " must be a number.");
        }
    }

    private void section(JPanel p, GridBagConstraints c, String text) {
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(10, 4, 2, 4);
        p.add(UIUtil.subtitle(text), c);
        c.gridwidth = 1;
        c.insets = new Insets(4, 4, 4, 4);
    }

    private void row(JPanel p, GridBagConstraints c, String label, Component field) {
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        p.add(field, c);
    }
}
