package driveshare.ui;

import driveshare.auth.SessionManager;
import driveshare.model.Booking;
import driveshare.model.BookingStatus;
import driveshare.payment.PaymentException;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Two tables:
 *  - My Trips (as renter): pay, cancel, message the owner
 *  - Requests for My Cars (as owner): confirm, decline, message the renter
 *
 * The Pay button shows the amount of the selected booking and goes through the PaymentProxy.
 */
public class BookingsPanel extends UIComponent {

    private final DefaultTableModel tripsModel = UIUtil.readOnlyModel(
            "#", "Car", "Owner", "From", "To", "Days", "Total", "Status");
    private final JTable tripsTable = UIUtil.table(tripsModel);
    private final List<Booking> trips = new ArrayList<>();

    private final DefaultTableModel requestsModel = UIUtil.readOnlyModel(
            "#", "Car", "Renter", "From", "To", "Days", "Total", "Status");
    private final JTable requestsTable = UIUtil.table(requestsModel);
    private final List<Booking> requests = new ArrayList<>();

    private final JButton payButton = UIUtil.primaryButton("Pay");

    public BookingsPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new BorderLayout());
        UIUtil.pad(this, 14);

        // ---- renter side ----
        JButton cancelButton = UIUtil.button("Cancel Booking");
        JButton messageOwnerButton = UIUtil.button("Message Owner");
        JPanel tripActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tripActions.add(messageOwnerButton);
        tripActions.add(cancelButton);
        tripActions.add(payButton);

        JPanel tripsPanel = new JPanel(new BorderLayout(0, 6));
        tripsPanel.add(UIUtil.title("My trips (as renter)"), BorderLayout.NORTH);
        tripsPanel.add(UIUtil.scroll(tripsTable), BorderLayout.CENTER);
        tripsPanel.add(tripActions, BorderLayout.SOUTH);

        // ---- owner side ----
        JButton confirmButton = UIUtil.primaryButton("Confirm Request");
        JButton declineButton = UIUtil.button("Decline Request");
        JButton messageRenterButton = UIUtil.button("Message Renter");
        JPanel requestActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        requestActions.add(messageRenterButton);
        requestActions.add(declineButton);
        requestActions.add(confirmButton);

        JPanel requestsPanel = new JPanel(new BorderLayout(0, 6));
        requestsPanel.add(UIUtil.title("Requests for my cars (as owner)"), BorderLayout.NORTH);
        requestsPanel.add(UIUtil.scroll(requestsTable), BorderLayout.CENTER);
        requestsPanel.add(requestActions, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tripsPanel, requestsPanel);
        split.setResizeWeight(0.5);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // the Pay button always shows the amount of the selected trip
        tripsTable.getSelectionModel().addListSelectionListener(e -> updatePayButton());

        payButton.addActionListener(e -> pay());
        cancelButton.addActionListener(e -> cancel());
        messageOwnerButton.addActionListener(e -> {
            Booking b = selected(tripsTable, trips);
            if (b != null) mediator.notify(this, UIEvent.OPEN_CHAT, b.getOwnerId());
        });
        confirmButton.addActionListener(e -> ownerAction(true));
        declineButton.addActionListener(e -> ownerAction(false));
        messageRenterButton.addActionListener(e -> {
            Booking b = selected(requestsTable, requests);
            if (b != null) mediator.notify(this, UIEvent.OPEN_CHAT, b.getRenterId());
        });
    }

    @Override
    public void refresh() {
        if (!SessionManager.getInstance().isLoggedIn()) return;
        fill(tripsModel, trips, services.bookings.getMyTrips(), true);
        fill(requestsModel, requests, services.bookings.getRequestsForMyCars(), false);
        updatePayButton();
    }

    private void fill(DefaultTableModel model, List<Booking> target, List<Booking> data, boolean renterView) {
        model.setRowCount(0);
        target.clear();
        target.addAll(data);
        for (Booking b : data) {
            model.addRow(new Object[] {
                b.getId(), b.getCarTitle(), renterView ? b.getOwnerName() : b.getRenterName(),
                b.getStartDate(), b.getEndDate(), b.getDays(), UIUtil.money(b.getTotalPrice()), b.getStatus()
            });
        }
    }

    private void updatePayButton() {
        int row = tripsTable.getSelectedRow();
        if (row >= 0 && row < trips.size() && trips.get(row).getStatus() == BookingStatus.CONFIRMED) {
            payButton.setText("Pay " + UIUtil.money(trips.get(row).getTotalPrice()));
            payButton.setEnabled(true);
        } else {
            payButton.setText("Pay");
            payButton.setEnabled(row >= 0);
        }
    }

    private Booking selected(JTable table, List<Booking> list) {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.error(this, "Please select a booking first.");
            return null;
        }
        return list.get(table.convertRowIndexToModel(row));
    }

    /** Payment goes through services.payment, which is the PaymentProxy. */
    private void pay() {
        Booking b = selected(tripsTable, trips);
        if (b == null) return;
        if (!UIUtil.confirm(this, "Pay " + UIUtil.money(b.getTotalPrice()) + " for the " + b.getCarTitle() + "?")) {
            return;
        }
        try {
            String receipt = services.payment.pay(b);
            UIUtil.info(this, receipt);
            mediator.notify(this, UIEvent.PAYMENT_COMPLETED, null);
        } catch (PaymentException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    private void cancel() {
        Booking b = selected(tripsTable, trips);
        if (b == null) return;
        if (!UIUtil.confirm(this, "Cancel your booking of the " + b.getCarTitle() + "?")) return;
        try {
            services.bookings.cancel(b);
            mediator.notify(this, UIEvent.BOOKING_CHANGED, null);
        } catch (IllegalArgumentException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    private void ownerAction(boolean confirm) {
        Booking b = selected(requestsTable, requests);
        if (b == null) return;
        try {
            if (confirm) services.bookings.confirm(b);
            else services.bookings.decline(b);
            mediator.notify(this, UIEvent.BOOKING_CHANGED, null);
        } catch (IllegalArgumentException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }
}
