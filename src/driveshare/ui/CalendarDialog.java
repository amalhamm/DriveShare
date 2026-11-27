package driveshare.ui;

import driveshare.model.Booking;
import driveshare.model.Car;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Availability calendar of one car, one month at a time.
 *   green = available, red = booked, gray = outside the owner's availability window (or paused).
 */
public class CalendarDialog extends JDialog {

    private static final Color AVAILABLE = new Color(205, 240, 215);
    private static final Color BOOKED = new Color(240, 140, 180);
    private static final Color UNAVAILABLE = new Color(225, 225, 230);

    private final Car car;
    private final List<Booking> bookings;
    private final JPanel grid = new JPanel(new GridLayout(0, 7, 3, 3));
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private YearMonth month;

    public CalendarDialog(Window owner, Car car, List<Booking> activeBookings) {
        super(owner, "Availability Calendar - " + car.getTitle(), ModalityType.APPLICATION_MODAL);
        this.car = car;
        this.bookings = activeBookings;
        LocalDate first = car.getAvailableFrom().isAfter(LocalDate.now()) ? car.getAvailableFrom() : LocalDate.now();
        this.month = YearMonth.from(first);

        JButton prev = UIUtil.button("<");
        JButton next = UIUtil.button(">");
        monthLabel.setFont(UIUtil.SUBTITLE);
        JPanel nav = new JPanel(new BorderLayout());
        nav.add(prev, BorderLayout.WEST);
        nav.add(monthLabel, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 2));
        legend.add(swatch(AVAILABLE, "Available"));
        legend.add(swatch(BOOKED, "Booked"));
        legend.add(swatch(UNAVAILABLE, "Not offered"));

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.add(UIUtil.muted("Offered " + car.getAvailableFrom() + " to " + car.getAvailableTo()
                + (car.isActive() ? "" : " (listing paused)")));
        info.add(legend);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        UIUtil.pad(content, 12);
        content.add(nav, BorderLayout.NORTH);
        content.add(grid, BorderLayout.CENTER);
        content.add(info, BorderLayout.SOUTH);
        setContentPane(content);

        prev.addActionListener(e -> { month = month.minusMonths(1); drawMonth(); });
        next.addActionListener(e -> { month = month.plusMonths(1); drawMonth(); });

        drawMonth();
        setSize(460, 400);
        setLocationRelativeTo(owner);
    }

    private void drawMonth() {
        grid.removeAll();
        monthLabel.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + month.getYear());

        for (DayOfWeek d : new DayOfWeek[] { DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY }) {
            JLabel h = new JLabel(d.getDisplayName(TextStyle.SHORT, Locale.US), SwingConstants.CENTER);
            h.setFont(h.getFont().deriveFont(Font.BOLD));
            grid.add(h);
        }

        int blanks = month.atDay(1).getDayOfWeek().getValue() % 7;   // Sunday = 0
        for (int i = 0; i < blanks; i++) grid.add(new JLabel(""));

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            JLabel cell = new JLabel(String.valueOf(day), SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setBackground(colorFor(date));
            if (date.equals(LocalDate.now())) {
                cell.setBorder(BorderFactory.createLineBorder(UIUtil.PRIMARY, 2));
            }
            grid.add(cell);
        }
        grid.revalidate();
        grid.repaint();
    }

    private Color colorFor(LocalDate date) {
        if (!car.isActive() || date.isBefore(car.getAvailableFrom()) || date.isAfter(car.getAvailableTo())) {
            return UNAVAILABLE;
        }
        for (Booking b : bookings) {
            if (!date.isBefore(b.getStartDate()) && !date.isAfter(b.getEndDate())) return BOOKED;
        }
        return AVAILABLE;
    }

    private static JPanel swatch(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JLabel box = new JLabel("    ");
        box.setOpaque(true);
        box.setBackground(color);
        p.add(box);
        p.add(new JLabel(text));
        return p;
    }
}
