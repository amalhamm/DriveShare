package driveshare.recovery;

/**
 * CHAIN OF RESPONSIBILITY PATTERN - abstract Handler.
 *
 * Each handler in the chain checks ONE thing. If its check passes it forwards the
 * request to the next handler; if the check fails the chain stops right there.
 * The request is approved only if it reaches the end of the chain.
 *
 * In DriveShare the chain has three handlers, one per security question:
 *
 *   Question 1 handler  -->  Question 2 handler  -->  Question 3 handler  -->  approved
 */
public abstract class RecoveryHandler {

    /** The next handler in the chain (null for the last one). */
    private RecoveryHandler next;

    /**
     * Links the next handler and returns it, so a chain can be built in one line:
     * first.setNext(second).setNext(third);
     */
    public RecoveryHandler setNext(RecoveryHandler next) {
        this.next = next;
        return next;
    }

    /**
     * Template of the chain: verify this step, then pass the request on.
     * @return true only if this handler AND every handler after it approved the request
     */
    public boolean handle(RecoveryRequest request) {
        if (!verify(request)) {
            return false;                     // stop the chain
        }
        if (next == null) {
            return true;                      // last handler: every check passed
        }
        return next.handle(request);          // forward to the next handler
    }

    /** The single check done by this handler. */
    protected abstract boolean verify(RecoveryRequest request);
}
