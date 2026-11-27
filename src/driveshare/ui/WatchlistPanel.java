package driveshare.ui;

import driveshare.auth.SessionManager;
import driveshare.model.Watch;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Renter's watch list (cars watched with price / availability conditions). */
public class WatchlistPanel extends UIComponent {

    private final DefaultTableModel model = UIUtil.readOnlyModel(
            "Car", "Notify when price per day is at or below", "Notify when available");
    private final JTable table = UIUtil.table(model);
    private final List<Watch> watches = new ArrayList<>();

    public WatchlistPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new BorderLayout(0, 10));
        UIUtil.pad(this, 14);

        JPanel top = new JPanel(new BorderLayout());
        top.add(UIUtil.title("Watchlist"), BorderLayout.NORTH);
        top.add(UIUtil.muted("Watch a car from the Search tab. You get a notification when it meets your conditions."),
                BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(UIUtil.scroll(table), BorderLayout.CENTER);

        JButton remove = UIUtil.button("Stop Watching");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(remove);
        add(actions, BorderLayout.SOUTH);

        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                UIUtil.error(this, "Please select a car first.");
                return;
            }
            services.watches.remove(watches.get(row));
            mediator.notify(this, UIEvent.WATCH_CHANGED, null);
        });
    }

    @Override
    public void refresh() {
        if (!SessionManager.getInstance().isLoggedIn()) return;
        model.setRowCount(0);
        watches.clear();
        watches.addAll(services.watches.getMyWatches());
        for (Watch w : watches) {
            model.addRow(new Object[] {
                w.getCarTitle(),
                w.getMaxPrice() == null ? "-" : UIUtil.money(w.getMaxPrice()),
                w.isNotifyWhenAvailable() ? "Yes" : "No"
            });
        }
    }
}
