package driveshare.auth;

import driveshare.db.UserDAO;
import driveshare.model.User;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * SINGLETON PATTERN - manages the logged-in user's session.
 *
 * There must be exactly one session in the running application, and every part of the
 * program (screens, booking service, payment proxy) must see the same one. So:
 *   - the constructor is private (no one else can call "new SessionManager()"),
 *   - the only instance is kept in a private static field,
 *   - getInstance() creates it the first time and always returns that same object.
 * getInstance() is synchronized so two threads can never create two instances.
 *
 * Security: when a user logs in, a random session token is generated with SecureRandom.
 * Logging out clears the user and the token. Code that needs a logged-in user calls
 * requireUser(), which throws if nobody is logged in.
 */
public final class SessionManager {

    /** The one and only instance (created lazily on the first call to getInstance). */
    private static SessionManager instance;

    private User currentUser;
    private String sessionToken;
    private LocalDateTime loginTime;

    /** Private constructor: other classes must use getInstance(). */
    private SessionManager() { }

    /** Global access point to the single SessionManager object. */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** Called by AuthService after the password was verified. */
    public synchronized void startSession(User user) {
        byte[] tokenBytes = new byte[32];
        new SecureRandom().nextBytes(tokenBytes);
        this.currentUser = user;
        this.sessionToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        this.loginTime = LocalDateTime.now();
        System.out.println("[SessionManager] Session started for " + user.getEmail() + " at " + loginTime.withNano(0));
    }

    /** Logs out: removes the user and invalidates the token. */
    public synchronized void endSession() {
        if (currentUser != null) {
            System.out.println("[SessionManager] Session ended for " + currentUser.getEmail());
        }
        currentUser = null;
        sessionToken = null;
        loginTime = null;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null && sessionToken != null;
    }

    /** @return the logged-in user, or null if nobody is logged in */
    public synchronized User getCurrentUser() {
        return currentUser;
    }

    /** @return the logged-in user; throws if the session is not valid */
    public synchronized User requireUser() {
        if (!isLoggedIn()) {
            throw new SecurityException("You must be logged in to do this.");
        }
        return currentUser;
    }

    /** Reloads the user from the database (e.g. after a payment changed the balance). */
    public synchronized void refreshUser() {
        if (currentUser != null) {
            User fresh = new UserDAO().findById(currentUser.getId());
            if (fresh != null) currentUser = fresh;
        }
    }

    public synchronized LocalDateTime getLoginTime() {
        return loginTime;
    }
}
