package driveshare.model;

/**
 * One of the three security questions a user sets at registration.
 * The answer is never stored in plain text: only a salted PBKDF2 hash is kept.
 */
public class SecurityQuestion {

    private final int userId;
    private final int order;          // 1, 2 or 3 - the position of this question in the recovery chain
    private final String question;
    private final String answerHash;
    private final String answerSalt;

    public SecurityQuestion(int userId, int order, String question, String answerHash, String answerSalt) {
        this.userId = userId;
        this.order = order;
        this.question = question;
        this.answerHash = answerHash;
        this.answerSalt = answerSalt;
    }

    public int getUserId() { return userId; }
    public int getOrder() { return order; }
    public String getQuestion() { return question; }
    public String getAnswerHash() { return answerHash; }
    public String getAnswerSalt() { return answerSalt; }
}
