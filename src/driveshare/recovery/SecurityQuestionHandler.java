package driveshare.recovery;

import driveshare.auth.PasswordHasher;
import driveshare.model.SecurityQuestion;

/**
 * CHAIN OF RESPONSIBILITY PATTERN - concrete Handler.
 *
 * Asks one security question and compares the answer with the stored hash.
 * Three of these are linked together to form the recovery chain.
 */
public class SecurityQuestionHandler extends RecoveryHandler {

    private final SecurityQuestion securityQuestion;

    public SecurityQuestionHandler(SecurityQuestion securityQuestion) {
        this.securityQuestion = securityQuestion;
    }

    @Override
    protected boolean verify(RecoveryRequest request) {
        int number = securityQuestion.getOrder();
        String answer = request.getAnswerProvider().askQuestion(number, securityQuestion.getQuestion());

        if (answer == null) {                 // user pressed Cancel
            request.setCancelled(true);
            return false;
        }

        boolean correct = PasswordHasher.verify(PasswordHasher.normalizeAnswer(answer),
                securityQuestion.getAnswerHash(), securityQuestion.getAnswerSalt());
        System.out.println("[Recovery] Question " + number + (correct ? " passed" : " FAILED -> chain stopped"));

        if (!correct) {
            request.setFailedQuestion(number);
        }
        return correct;
    }
}
