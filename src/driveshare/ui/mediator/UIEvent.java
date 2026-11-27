package driveshare.ui.mediator;

/** Events the screens send to the mediator. */
public enum UIEvent {
    SHOW_LOGIN,
    SHOW_REGISTER,
    REGISTERED,          // data = email of the new account
    LOGIN_SUCCESS,
    LOGOUT,
    LISTING_SAVED,
    BOOKING_CHANGED,
    PAYMENT_COMPLETED,
    WATCH_CHANGED,
    OPEN_CHAT,           // data = id of the user to chat with
    MESSAGE_SENT,
    NOTIFICATIONS_READ,
    SHOW_NOTIFICATIONS
}
