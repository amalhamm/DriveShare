package driveshare;

import driveshare.db.Database;
import driveshare.service.Services;
import driveshare.ui.MainFrame;
import driveshare.ui.UIUtil;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * DriveShare - Peer-to-Peer Car Rental Platform (CIS 476 Term Project).
 *
 * Start-up: create / open the SQLite database, create the services, then show the window.
 *
 * Design patterns used:
 *   Singleton               auth.SessionManager
 *   Observer                observer.CarWatchNotifier (subject), observer.WatchObserver (observer)
 *   Mediator                ui.mediator.DriveShareMediator + the screens (UIComponent)
 *   Builder                 model.CarListingBuilder builds model.Car
 *   Proxy                   payment.PaymentProxy in front of payment.RealPaymentService
 *   Chain of Responsibility recovery.SecurityQuestionHandler x 3 for password recovery
 */
public class Main {

    public static void main(String[] args) {
        try {
            Database.initialize();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null, "Could not open the database:\n" + e.getMessage()
                    + "\n\nMake sure lib/sqlite-jdbc-3.46.1.3.jar is on the classpath.",
                    "DriveShare", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            UIUtil.applyLookAndFeel();
            Services services = new Services();
            new MainFrame(services).setVisible(true);
        });
    }
}
