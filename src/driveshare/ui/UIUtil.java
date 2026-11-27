package driveshare.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Shared colors, fonts and small helpers so every screen looks the same. */
public final class UIUtil {

    public static final Color PRIMARY = new Color(232, 98, 160);       // pink
    public static final Color PRIMARY_DARK = new Color(196, 62, 128);  // darker pink for titles
    public static final Color BACKGROUND = new Color(255, 244, 249);   // very light pink
    public static final Color BORDER = new Color(246, 198, 222);       // soft pink border
    public static final Color TEXT_MUTED = new Color(105, 105, 120);
    public static final Color SUCCESS = new Color(40, 150, 90);
    public static final Color DANGER = new Color(200, 55, 70);

    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font SUBTITLE = new Font("SansSerif", Font.BOLD, 15);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    private UIUtil() { }

    /** Uses the Nimbus look and feel with the DriveShare purple as its base color. */
    public static void applyLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
            UIManager.put("nimbusBase", PRIMARY_DARK);
            UIManager.put("nimbusFocus", PRIMARY);
            UIManager.put("nimbusSelectionBackground", PRIMARY);
            UIManager.put("control", BACKGROUND);
            UIManager.put("nimbusBlueGrey", new Color(214, 150, 184));
            UIManager.put("nimbusLightBackground", Color.WHITE);
        } catch (Exception ignored) {
            // the default look and feel is fine if Nimbus is not available
        }
    }

    public static String money(double amount) {
        return MONEY.format(amount);
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TITLE);
        label.setForeground(PRIMARY_DARK);
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SUBTITLE);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    /** A filled purple button for the main action of a screen. */
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.BOLD));
        b.setForeground(Color.WHITE);
        b.setBackground(PRIMARY);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** A button that looks like a link (no border). */
    public static JButton linkButton(String text) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(PRIMARY);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Table model whose cells cannot be edited. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** A table with single-row selection and comfortable row height. */
    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(26);
        t.setFont(BODY);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.setFillsViewportHeight(true);
        return t;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        return sp;
    }

    /** Date picker showing yyyy-MM-dd. */
    public static JSpinner dateSpinner(LocalDate initial) {
        Date date = Date.from(initial.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner spinner = new JSpinner(new SpinnerDateModel(date, null, null, Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    public static LocalDate dateOf(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JSpinner spinner, LocalDate value) {
        spinner.setValue(Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "DriveShare", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "DriveShare", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "DriveShare", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    /** Parses an optional money field: empty = null. */
    public static Double parseOptionalMoney(String text) {
        String t = text == null ? "" : text.trim().replace("$", "");
        if (t.isEmpty()) return null;
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a valid amount.");
        }
    }

    /** Padding around a panel. */
    public static void pad(JComponent c, int size) {
        c.setBorder(BorderFactory.createEmptyBorder(size, size, size, size));
    }
}
