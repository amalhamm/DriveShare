package driveshare.recovery;

/**
 * How a handler asks the user a question. The GUI implements this with a dialog box,
 * so the recovery chain does not depend on any Swing class.
 */
public interface AnswerProvider {

    /**
     * @param questionNumber 1, 2 or 3
     * @param question       the question text the user chose at registration
     * @return the typed answer, or null if the user pressed Cancel
     */
    String askQuestion(int questionNumber, String question);
}
