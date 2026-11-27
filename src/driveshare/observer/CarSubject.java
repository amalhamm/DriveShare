package driveshare.observer;

import driveshare.model.Car;

/** OBSERVER PATTERN - Subject interface: keeps a list of observers and notifies them. */
public interface CarSubject {

    void attach(CarObserver observer);

    void detach(CarObserver observer);

    /** Tells every observer of this car that something changed. */
    void notifyObservers(Car car, CarEvent event);
}
