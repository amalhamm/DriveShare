package driveshare.recovery;

import driveshare.db.UserDAO;
import driveshare.model.SecurityQuestion;
import driveshare.model.User;

import java.util.List;

/**
 * Client of the Chain of Responsibility: builds the chain from the user's three
 * security questions and sends the recovery request into the first handler.
 */
public class PasswordRecoveryService {

    private final UserDAO userDAO = new UserDAO();

    /** @return the account for this email, or null if none exists */
    public User findUser(String email) {
        return email == null ? null : userDAO.findByEmail(email.trim());
    }

    /** Builds: question 1 handler -> question 2 handler -> question 3 handler. */
    public RecoveryHandler buildChain(User user) {
        List<SecurityQuestion> questions = userDAO.getSecurityQuestions(user.getId());
        if (questions.size() != 3) {
            throw new IllegalStateException("This account does not have three security questions.");
        }
        RecoveryHandler first = new SecurityQuestionHandler(questions.get(0));
        first.setNext(new SecurityQuestionHandler(questions.get(1)))
             .setNext(new SecurityQuestionHandler(questions.get(2)));
        return first;
    }

    /**
     * Runs the whole chain for a user.
     * @return true if all three answers were correct (the password may then be reset)
     */
    public boolean verifyIdentity(RecoveryRequest request) {
        return buildChain(request.getUser()).handle(request);
    }
}
