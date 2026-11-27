package driveshare.observer;

import driveshare.model.Car;

/** OBSERVER PATTERN - Observer interface: anything that wants to hear about changes to a car. */
public interface CarObserver {

    /** Called by the subject when a watched car changes. */
    void update(Car car, CarEvent event);

    /** Id of the car this observer is watching. */
    int getCarId();
}
