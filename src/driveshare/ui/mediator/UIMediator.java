package driveshare.ui.mediator;

/**
 * MEDIATOR PATTERN - Mediator interface.
 *
 * Screens (colleagues) never call each other. When something happens on a screen, it
 * tells the mediator, and the mediator decides which other screens must react.
 */
public interface UIMediator {

    /**
     * @param sender the screen where the event happened
     * @param event  what happened
     * @param data   optional extra information (for example a user id), may be null
     */
    void notify(UIComponent sender, UIEvent event, Object data);
}
