package driveshare.ui.mediator;

import driveshare.service.Services;

import javax.swing.JPanel;

/**
 * MEDIATOR PATTERN - abstract Colleague.
 *
 * Every screen extends this class. It only knows the mediator (to report events) and the
 * services (to do its own work) - it holds no reference to any other screen.
 */
public abstract class UIComponent extends JPanel {

    protected final UIMediator mediator;
    protected final Services services;

    protected UIComponent(UIMediator mediator, Services services) {
        this.mediator = mediator;
        this.services = services;
    }

    /** Reloads the data shown on this screen. The mediator calls it when other screens change data. */
    public void refresh() { }
}
