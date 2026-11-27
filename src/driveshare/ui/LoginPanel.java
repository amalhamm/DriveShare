package driveshare.ui;

import driveshare.model.User;
import driveshare.recovery.RecoveryRequest;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Login screen, with links to registration and to password recovery.
 * Password recovery runs the Chain of Responsibility (three security-question handlers).
 */
public class LoginPanel extends UIComponent {

    private final JTextField emailField = new JTextField(22);
    private final JPasswordField passwordField = new JPasswordField(22);

    public LoginPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new GridBagLayout());
        setBackground(UIUtil.BACKGROUND);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 2),
                BorderFactory.createEmptyBorder(28, 36, 28, 36)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);

        JLabel title = UIUtil.title("DriveShare");
        title.setFont(title.getFont().deriveFont(30f));
        card.add(title, c);
        card.add(UIUtil.muted("Rent cars directly from their owners"), c);
        c.insets = new Insets(16, 0, 2, 0);
        card.add(new JLabel("Email"), c);
        c.insets = new Insets(2, 0, 4, 0);
        card.add(emailField, c);
        c.insets = new Insets(8, 0, 2, 0);
        card.add(new JLabel("Password"), c);
        c.insets = new Insets(2, 0, 4, 0);
        card.add(passwordField, c);

        JButton loginButton = UIUtil.primaryButton("Log In");
        c.insets = new Insets(14, 0, 4, 0);
        card.add(loginButton, c);

        JPanel links = new JPanel(new GridLayout(1, 2));
        links.setOpaque(false);
        JButton registerLink = UIUtil.linkButton("Create an account");
        JButton forgotLink = UIUtil.linkButton("Forgot password?");
        links.add(registerLink);
        links.add(forgotLink);
        c.insets = new Insets(4, 0, 4, 0);
        card.add(links, c);

        JLabel demo = UIUtil.muted("<html><center>Demo accounts:<br>renter adam@driveshare.com / renter123"
                + "<br>owner sara@driveshare.com / owner123</center></html>");
        demo.setHorizontalAlignment(JLabel.CENTER);
        c.insets = new Insets(12, 0, 0, 0);
        card.add(demo, c);

        add(card);

        loginButton.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());       // Enter key logs in
        registerLink.addActionListener(e -> mediator.notify(this, UIEvent.SHOW_REGISTER, null));
        forgotLink.addActionListener(e -> recoverPassword());
    }

    private void login() {
        try {
            services.auth.login(emailField.getText(), new String(passwordField.getPassword()));
            passwordField.setText("");
            mediator.notify(this, UIEvent.LOGIN_SUCCESS, null);
        } catch (IllegalArgumentException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    /**
     * Password recovery: the request goes through the chain
     * question 1 -> question 2 -> question 3. Only if all three handlers approve can the
     * user choose a new password.
     */
    private void recoverPassword() {
        String email = JOptionPane.showInputDialog(this, "Enter your account email:",
                "Password Recovery", JOptionPane.QUESTION_MESSAGE);
        if (email == null) return;

        User user = services.recovery.findUser(email);
        if (user == null) {
            UIUtil.error(this, "No account was found for that email.");
            return;
        }

        // The AnswerProvider lets each handler ask its question with a dialog box.
        RecoveryRequest request = new RecoveryRequest(user, (number, question) ->
                JOptionPane.showInputDialog(this, question, "Security Question " + number + " of 3",
                        JOptionPane.QUESTION_MESSAGE));

        boolean verified = services.recovery.verifyIdentity(request);
        if (request.isCancelled()) return;
        if (!verified) {
            UIUtil.error(this, "Incorrect answer to security question " + request.getFailedQuestion()
                    + ".\nPassword recovery was stopped.");
            return;
        }

        // all three handlers approved - let the user choose a new password
        JPasswordField newPassword = new JPasswordField(18);
        JPasswordField confirm = new JPasswordField(18);
        JPanel form = new JPanel(new GridLayout(4, 1, 4, 4));
        form.add(new JLabel("All three answers are correct. New password:"));
        form.add(newPassword);
        form.add(new JLabel("Confirm new password:"));
        form.add(confirm);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(this, form, "Reset Password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return;
            try {
                services.auth.resetPassword(user.getId(), new String(newPassword.getPassword()),
                        new String(confirm.getPassword()));
                emailField.setText(user.getEmail());
                UIUtil.info(this, "Your password was reset. You can log in now.");
                return;
            } catch (IllegalArgumentException ex) {
                UIUtil.error(this, ex.getMessage());
            }
        }
    }

    public void prefillEmail(String email) {
        emailField.setText(email);
        passwordField.setText("");
        passwordField.requestFocusInWindow();
    }

    public void clear() {
        emailField.setText("");
        passwordField.setText("");
    }
}
