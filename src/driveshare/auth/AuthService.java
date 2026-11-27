package driveshare.auth;

import driveshare.db.UserDAO;
import driveshare.model.SecurityQuestion;
import driveshare.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Registration, login and password reset.
 * Passwords and security answers are hashed with PasswordHasher before they are stored.
 */
public class AuthService {

    /** The questions offered on the registration screen. */
    public static final String[] QUESTION_CHOICES = {
        "What city were you born in?",
        "What was the name of your first pet?",
        "What is your favorite food?",
        "What was the name of your elementary school?",
        "What is your mother's maiden name?",
        "What was the make of your first car?"
    };

    /** Every new account starts with this simulated wallet balance so payments can be demonstrated. */
    public static final double STARTING_BALANCE = 1000.00;

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final UserDAO userDAO = new UserDAO();

    /**
     * Creates an account. The user must give three DIFFERENT security questions with answers.
     * @throws IllegalArgumentException with a message the screen can show
     */
    public User register(String email, String fullName, String password, String confirmPassword,
                         String[] questions, String[] answers) {
        email = email == null ? "" : email.trim();
        fullName = fullName == null ? "" : fullName.trim();

        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("Please enter a valid email address.");
        if (fullName.isEmpty()) throw new IllegalArgumentException("Please enter your full name.");
        validatePassword(password, confirmPassword);
        if (questions == null || answers == null || questions.length != 3 || answers.length != 3)
            throw new IllegalArgumentException("Three security questions are required.");

        Set<String> distinct = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            if (questions[i] == null || questions[i].trim().isEmpty())
                throw new IllegalArgumentException("Please choose security question " + (i + 1) + ".");
            if (PasswordHasher.normalizeAnswer(answers[i]).isEmpty())
                throw new IllegalArgumentException("Please answer security question " + (i + 1) + ".");
            distinct.add(questions[i]);
        }
        if (distinct.size() != 3) throw new IllegalArgumentException("Please choose three different security questions.");
        if (userDAO.emailExists(email)) throw new IllegalArgumentException("An account with this email already exists.");

        // hash the password and each answer with its own salt
        String passwordSalt = PasswordHasher.newSalt();
        String passwordHash = PasswordHasher.hash(password, passwordSalt);
        List<SecurityQuestion> secured = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String salt = PasswordHasher.newSalt();
            String hash = PasswordHasher.hash(PasswordHasher.normalizeAnswer(answers[i]), salt);
            secured.add(new SecurityQuestion(0, i + 1, questions[i], hash, salt));
        }

        int id = userDAO.insertUser(email, fullName, passwordHash, passwordSalt, STARTING_BALANCE, secured);
        return userDAO.findById(id);
    }

    /**
     * Checks the email/password and, if correct, starts the session in the SessionManager singleton.
     * @throws IllegalArgumentException if the login fails (same message for wrong email or password)
     */
    public User login(String email, String password) {
        String[] stored = userDAO.getPasswordHashAndSalt(email == null ? "" : email.trim());
        if (stored == null || password == null || !PasswordHasher.verify(password, stored[0], stored[1])) {
            throw new IllegalArgumentException("Incorrect email or password.");
        }
        User user = userDAO.findByEmail(email.trim());
        SessionManager.getInstance().startSession(user);
        return user;
    }

    public void logout() {
        SessionManager.getInstance().endSession();
    }

    /** Stores a new password (only called after the recovery chain succeeded). */
    public void resetPassword(int userId, String newPassword, String confirmPassword) {
        validatePassword(newPassword, confirmPassword);
        String salt = PasswordHasher.newSalt();
        userDAO.updatePassword(userId, PasswordHasher.hash(newPassword, salt), salt);
    }

    private void validatePassword(String password, String confirm) {
        if (password == null || password.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        if (!password.equals(confirm))
            throw new IllegalArgumentException("Passwords do not match.");
    }
}
