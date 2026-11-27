package driveshare.ui;

import driveshare.auth.AuthService;
import driveshare.model.User;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Registration screen: email + password and three security questions with answers. */
public class RegisterPanel extends UIComponent {

    private final JTextField nameField = new JTextField(24);
    private final JTextField emailField = new JTextField(24);
    private final JPasswordField passwordField = new JPasswordField(24);
    private final JPasswordField confirmField = new JPasswordField(24);
    private final JComboBox<String>[] questionBoxes;
    private final JTextField[] answerFields = new JTextField[3];

    @SuppressWarnings("unchecked")
    public RegisterPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new GridBagLayout());
        setBackground(UIUtil.BACKGROUND);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BORDER, 2),
                BorderFactory.createEmptyBorder(22, 32, 22, 32)));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        card.add(UIUtil.title("Create your DriveShare account"), c);
        c.gridy++;
        card.add(UIUtil.muted("Every new account starts with a demo balance of "
                + UIUtil.money(AuthService.STARTING_BALANCE) + "."), c);
        c.gridwidth = 1;

        addRow(card, c, "Full name", nameField);
        addRow(card, c, "Email", emailField);
        addRow(card, c, "Password (min. 6)", passwordField);
        addRow(card, c, "Confirm password", confirmField);

        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        c.insets = new Insets(14, 6, 2, 6);
        card.add(UIUtil.subtitle("Security questions (used to recover a forgotten password)"), c);
        c.insets = new Insets(4, 6, 4, 6);
        c.gridwidth = 1;

        questionBoxes = new JComboBox[3];
        for (int i = 0; i < 3; i++) {
            questionBoxes[i] = new JComboBox<>(AuthService.QUESTION_CHOICES);
            questionBoxes[i].setSelectedIndex(i);       // start with three different questions
            answerFields[i] = new JTextField(24);
            addRow(card, c, "Question " + (i + 1), questionBoxes[i]);
            addRow(card, c, "Answer " + (i + 1), answerFields[i]);
        }

        JButton create = UIUtil.primaryButton("Create Account");
        JButton back = UIUtil.button("Back to Login");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(back);
        buttons.add(create);
        c.gridx = 0; c.gridy++; c.gridwidth = 2;
        c.insets = new Insets(14, 6, 0, 6);
        card.add(buttons, c);

        add(card);

        create.addActionListener(e -> register());
        back.addActionListener(e -> mediator.notify(this, UIEvent.SHOW_LOGIN, null));
    }

    private void addRow(JPanel panel, GridBagConstraints c, String label, java.awt.Component field) {
        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(field, c);
    }

    private void register() {
        String[] questions = new String[3];
        String[] answers = new String[3];
        for (int i = 0; i < 3; i++) {
            questions[i] = (String) questionBoxes[i].getSelectedItem();
            answers[i] = answerFields[i].getText();
        }
        try {
            User user = services.auth.register(emailField.getText(), nameField.getText(),
                    new String(passwordField.getPassword()), new String(confirmField.getPassword()),
                    questions, answers);
            UIUtil.info(this, "Account created for " + user.getFullName() + ". Please log in.");
            mediator.notify(this, UIEvent.REGISTERED, user.getEmail());
        } catch (IllegalArgumentException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }

    public void clear() {
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        for (int i = 0; i < 3; i++) {
            questionBoxes[i].setSelectedIndex(i);
            answerFields[i].setText("");
        }
    }
}
