package driveshare.ui.mediator;

import driveshare.service.Services;
import driveshare.ui.BookingsPanel;
import driveshare.ui.HeaderPanel;
import driveshare.ui.LoginPanel;
import driveshare.ui.MainFrame;
import driveshare.ui.MessagesPanel;
import driveshare.ui.MyCarsPanel;
import driveshare.ui.NotificationsPanel;
import driveshare.ui.RegisterPanel;
import driveshare.ui.SearchPanel;
import driveshare.ui.WatchlistPanel;

import javax.swing.JTabbedPane;
import java.awt.Component;

/**
 * MEDIATOR PATTERN - Concrete Mediator.
 *
 * Knows every screen and holds all the rules about how they interact. For example, when the
 * renter pays on the Bookings screen, the mediator refreshes the balance in the header, the
 * bookings tables and the notifications list - the Bookings screen itself knows none of them.
 *
 * Without the mediator each screen would need references to many others (a web of
 * dependencies). With it, each screen depends only on the mediator.
 */
public class DriveShareMediator implements UIMediator {

    private final Services services;

    // the colleagues, registered by MainFrame after they are created
    private MainFrame frame;
    private JTabbedPane tabs;
    private LoginPanel login;
    private RegisterPanel register;
    private HeaderPanel header;
    private SearchPanel search;
    private MyCarsPanel myCars;
    private BookingsPanel bookings;
    private WatchlistPanel watchlist;
    private MessagesPanel messages;
    private NotificationsPanel notifications;

    public DriveShareMediator(Services services) {
        this.services = services;
    }

    /** Called once by MainFrame to introduce all the screens to the mediator. */
    public void registerComponents(MainFrame frame, JTabbedPane tabs, LoginPanel login, RegisterPanel register,
                                   HeaderPanel header, SearchPanel search, MyCarsPanel myCars,
                                   BookingsPanel bookings, WatchlistPanel watchlist, MessagesPanel messages,
                                   NotificationsPanel notifications) {
        this.frame = frame;
        this.tabs = tabs;
        this.login = login;
        this.register = register;
        this.header = header;
        this.search = search;
        this.myCars = myCars;
        this.bookings = bookings;
        this.watchlist = watchlist;
        this.messages = messages;
        this.notifications = notifications;

        // switching tabs is also coordinated here: the newly shown screen reloads its data
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected instanceof UIComponent) {
                ((UIComponent) selected).refresh();
                header.refresh();
            }
        });
    }

    @Override
    public void notify(UIComponent sender, UIEvent event, Object data) {
        System.out.println("[Mediator] " + event + " from " + sender.getClass().getSimpleName());

        switch (event) {
            case SHOW_LOGIN:
                frame.showScreen(MainFrame.LOGIN);
                break;

            case SHOW_REGISTER:
                register.clear();
                frame.showScreen(MainFrame.REGISTER);
                break;

            case REGISTERED:
                login.prefillEmail((String) data);
                frame.showScreen(MainFrame.LOGIN);
                break;

            case LOGIN_SUCCESS:
                refreshAll();
                tabs.setSelectedComponent(search);
                frame.showScreen(MainFrame.MAIN);
                break;

            case LOGOUT:
                services.auth.logout();
                login.clear();
                messages.clear();
                frame.showScreen(MainFrame.LOGIN);
                break;

            case LISTING_SAVED:
                myCars.refresh();
                search.refresh();
                watchlist.refresh();
                header.refresh();
                break;

            case BOOKING_CHANGED:
                bookings.refresh();
                search.refresh();
                messages.refresh();           // the other side of a booking becomes a chat contact
                notifications.refresh();
                header.refresh();
                break;

            case PAYMENT_COMPLETED:
                header.refresh();              // new balance
                bookings.refresh();            // status becomes PAID
                notifications.refresh();
                break;

            case WATCH_CHANGED:
                watchlist.refresh();
                break;

            case OPEN_CHAT:
                messages.openConversation((Integer) data);
                tabs.setSelectedComponent(messages);
                break;

            case MESSAGE_SENT:
            case NOTIFICATIONS_READ:
                notifications.refresh();
                header.refresh();
                break;

            case SHOW_NOTIFICATIONS:
                tabs.setSelectedComponent(notifications);
                break;

            default:
                break;
        }
    }

    private void refreshAll() {
        header.refresh();
        search.refresh();
        myCars.refresh();
        bookings.refresh();
        watchlist.refresh();
        messages.refresh();
        notifications.refresh();
    }
}
