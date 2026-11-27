package driveshare.ui;

import driveshare.auth.SessionManager;
import driveshare.model.Message;
import driveshare.model.User;
import driveshare.service.Services;
import driveshare.ui.mediator.UIComponent;
import driveshare.ui.mediator.UIEvent;
import driveshare.ui.mediator.UIMediator;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;

/** Messaging between owners and renters: contacts on the left, conversation on the right. */
public class MessagesPanel extends UIComponent {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final DefaultListModel<User> contactsModel = new DefaultListModel<>();
    private final JList<User> contactsList = new JList<>(contactsModel);
    private final JTextArea conversation = new JTextArea();
    private final JTextField input = new JTextField();
    private final JButton sendButton = UIUtil.primaryButton("Send");

    public MessagesPanel(UIMediator mediator, Services services) {
        super(mediator, services);
        setLayout(new BorderLayout(0, 10));
        UIUtil.pad(this, 14);
        add(UIUtil.title("Messages"), BorderLayout.NORTH);

        contactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contactsList.setFont(UIUtil.BODY.deriveFont(14f));
        contactsList.setFixedCellHeight(30);
        JPanel left = new JPanel(new BorderLayout(0, 6));
        left.add(UIUtil.subtitle("Conversations"), BorderLayout.NORTH);
        left.add(UIUtil.scroll(contactsList), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(260, 100));

        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(UIUtil.BODY.deriveFont(14f));
        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);
        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.add(UIUtil.scroll(conversation), BorderLayout.CENTER);
        right.add(inputRow, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        contactsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showConversation();
        });
        sendButton.addActionListener(e -> send());
        input.addActionListener(e -> send());
    }

    @Override
    public void refresh() {
        if (!SessionManager.getInstance().isLoggedIn()) return;
        User selected = contactsList.getSelectedValue();
        contactsModel.clear();
        for (User u : services.messages.getContacts()) contactsModel.addElement(u);
        if (selected == null || !select(selected.getId())) {
            if (!contactsModel.isEmpty()) contactsList.setSelectedIndex(0);   // open the first conversation
        }
        showConversation();
    }

    /** Called by the mediator (e.g. "Message Owner" on another screen) to open a chat with a user. */
    public void openConversation(int userId) {
        refresh();
        if (!select(userId)) {
            User user = services.messages.findUser(userId);   // new contact with no messages yet
            if (user != null) {
                contactsModel.addElement(user);
                select(userId);
            }
        }
        input.requestFocusInWindow();
    }

    public void clear() {
        contactsModel.clear();
        conversation.setText("");
        input.setText("");
    }

    private boolean select(int userId) {
        for (int i = 0; i < contactsModel.size(); i++) {
            if (contactsModel.get(i).getId() == userId) {
                contactsList.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    private void showConversation() {
        User other = contactsList.getSelectedValue();
        if (other == null) {
            conversation.setText("Select a conversation, or use \"Message Owner\" / \"Message Renter\" "
                    + "on the Search or Bookings tab to start one.");
            return;
        }
        int me = SessionManager.getInstance().requireUser().getId();
        StringBuilder sb = new StringBuilder();
        for (Message m : services.messages.getConversation(other.getId())) {
            sb.append(m.getSenderId() == me ? "You" : m.getSenderName())
              .append("  (").append(m.getSentAt().format(TIME)).append(")\n")
              .append(m.getBody()).append("\n\n");
        }
        conversation.setText(sb.length() == 0 ? "No messages yet with " + other.getFullName() + "." : sb.toString());
        conversation.setCaretPosition(conversation.getDocument().getLength());
    }

    private void send() {
        User other = contactsList.getSelectedValue();
        if (other == null) {
            UIUtil.error(this, "Please choose who to message first.");
            return;
        }
        try {
            services.messages.send(other.getId(), input.getText());
            input.setText("");
            showConversation();
            mediator.notify(this, UIEvent.MESSAGE_SENT, null);
        } catch (IllegalArgumentException ex) {
            UIUtil.error(this, ex.getMessage());
        }
    }
}
