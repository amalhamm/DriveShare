package driveshare.ui;

import driveshare.service.Services;
import driveshare.ui.mediator.DriveShareMediator;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/**
 * The application window. It holds three screens in a CardLayout:
 * LOGIN, REGISTER and MAIN (header + tabs). It creates every screen, gives each one the
 * mediator, and registers them all with the mediator.
 */
public class MainFrame extends JFrame {

    public static final String LOGIN = "login";
    public static final String REGISTER = "register";
    public static final String MAIN = "main";

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public MainFrame(Services services) {
        super("DriveShare - Peer-to-Peer Car Rental");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        DriveShareMediator mediator = new DriveShareMediator(services);

        // colleagues - each one only receives the mediator, never another screen
        LoginPanel login = new LoginPanel(mediator, services);
        RegisterPanel register = new RegisterPanel(mediator, services);
        HeaderPanel header = new HeaderPanel(mediator, services);
        SearchPanel search = new SearchPanel(mediator, services);
        MyCarsPanel myCars = new MyCarsPanel(mediator, services);
        BookingsPanel bookings = new BookingsPanel(mediator, services);
        WatchlistPanel watchlist = new WatchlistPanel(mediator, services);
        MessagesPanel messages = new MessagesPanel(mediator, services);
        NotificationsPanel notifications = new NotificationsPanel(mediator, services);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIUtil.BODY.deriveFont(14f));
        tabs.addTab("Search Cars", search);
        tabs.addTab("My Listings", myCars);
        tabs.addTab("Bookings", bookings);
        tabs.addTab("Watchlist", watchlist);
        tabs.addTab("Messages", messages);
        tabs.addTab("Notifications", notifications);

        JPanel main = new JPanel(new BorderLayout());
        main.add(header, BorderLayout.NORTH);
        main.add(tabs, BorderLayout.CENTER);

        root.add(login, LOGIN);
        root.add(register, REGISTER);
        root.add(main, MAIN);
        setContentPane(root);

        mediator.registerComponents(this, tabs, login, register, header, search, myCars, bookings,
                watchlist, messages, notifications);

        setSize(1180, 720);
        setLocationRelativeTo(null);
        showScreen(LOGIN);
    }

    /** Switches between LOGIN, REGISTER and MAIN. Only the mediator calls this. */
    public void showScreen(String name) {
        cards.show(root, name);
    }
}
