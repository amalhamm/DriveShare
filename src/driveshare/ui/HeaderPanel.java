package driveshare.ui;

import driveshare.auth.SessionManager;
import driveshare.model.User;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

/** Top bar: user name, wallet balance, unread notification count and Log Out. */
public class HeaderPanel extends UIComponent {

    private final JLabel userLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JButton notificationsButton = UIUtil.button("Notifications");
    private final JButton logoutButton = UIUtil.button("Log Out");

    public HeaderPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new BorderLayout());
        setBackground(UIUtil.PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JLabel logo = new JLabel("DriveShare");
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        logo.setForeground(Color.WHITE);
        add(logo, BorderLayout.WEST);

        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(UIUtil.BODY);
        balanceLabel.setForeground(Color.WHITE);
        balanceLabel.setFont(UIUtil.BODY.deriveFont(Font.BOLD, 14f));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(userLabel);
        right.add(balanceLabel);
        right.add(notificationsButton);
        right.add(logoutButton);
        add(right, BorderLayout.EAST);

        notificationsButton.addActionListener(e -> mediator.notify(this, UIEvent.SHOW_NOTIFICATIONS, null));
        logoutButton.addActionListener(e -> mediator.notify(this, UIEvent.LOGOUT, null));
    }

    @Override
    public void refresh() {
        SessionManager session = SessionManager.getInstance();
        if (!session.isLoggedIn()) return;
        session.refreshUser();
        User user = session.getCurrentUser();
        userLabel.setText("Hi, " + user.getFullName());
        balanceLabel.setText("Balance: " + UIUtil.money(user.getBalance()));
        int unread = services.notifications.countUnread(user.getId());
        notificationsButton.setText(unread > 0 ? "Notifications (" + unread + ")" : "Notifications");
    }
}
