
package com.telemetry.events;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class TelemetryEventBus {

    private final CopyOnWriteArrayList<TelemetryRecorder> observers =
        new CopyOnWriteArrayList<>();

    public void subscribe(TelemetryRecorder observer) {
        observers.addIfAbsent(
                Objects.requireNonNull(observer)
        );
    }

    public void unsubscribe(TelemetryRecorder observer) {
        observers.remove(observer);
    }

    public void publish(TelemetryEvent event) {
        Objects.requireNonNull(event);

        for (TelemetryRecorder observer : observers) {
            observer.recordEvent(event);
        }
    }
}
