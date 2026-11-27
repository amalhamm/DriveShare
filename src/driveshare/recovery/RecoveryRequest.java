package driveshare.recovery;

import driveshare.model.User;

/**
 * The request that travels along the password-recovery chain.
 * It carries the user who forgot the password and the AnswerProvider used to ask questions.
 * If a handler rejects the request it records which question failed.
 */
public class RecoveryRequest {

    private final User user;
    private final AnswerProvider answerProvider;
    private int failedQuestion;      // 0 = no failure
    private boolean cancelled;

    public RecoveryRequest(User user, AnswerProvider answerProvider) {
        this.user = user;
        this.answerProvider = answerProvider;
    }

    public User getUser() { return user; }
    public AnswerProvider getAnswerProvider() { return answerProvider; }

    public int getFailedQuestion() { return failedQuestion; }
    public void setFailedQuestion(int questionNumber) { this.failedQuestion = questionNumber; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}
