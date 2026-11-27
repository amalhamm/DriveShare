package driveshare.ui;

import driveshare.auth.SessionManager;
import driveshare.model.Notification;
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
import java.time.format.DateTimeFormatter;

/** In-app notifications: booking requests, confirmations, payments, watch alerts and new messages. */
public class NotificationsPanel extends UIComponent {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final DefaultTableModel model = UIUtil.readOnlyModel("", "Time", "Notification");
    private final JTable table = UIUtil.table(model);

    public NotificationsPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new BorderLayout(0, 10));
        UIUtil.pad(this, 14);
        add(UIUtil.title("Notifications"), BorderLayout.NORTH);

        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setMaxWidth(160);
        add(UIUtil.scroll(table), BorderLayout.CENTER);

        JButton markRead = UIUtil.button("Mark All as Read");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(markRead);
        add(actions, BorderLayout.SOUTH);

        markRead.addActionListener(e -> {
            services.notifications.markAllRead(SessionManager.getInstance().requireUser().getId());
            mediator.notify(this, UIEvent.NOTIFICATIONS_READ, null);
        });
    }

    @Override
    public void refresh() {
        if (!SessionManager.getInstance().isLoggedIn()) return;
        model.setRowCount(0);
        for (Notification n : services.notifications.getNotifications(SessionManager.getInstance().getCurrentUser().getId())) {
            model.addRow(new Object[] { n.isRead() ? "" : "NEW", n.getCreatedAt().format(TIME), n.getMessage() });
        }
    }
}
