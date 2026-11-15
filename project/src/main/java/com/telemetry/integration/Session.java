package com.telemetry.integration;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Session {
    private String project;
    private String studentId = "STUDENT_ID";
    private Path projectDirectory;
    private LocalDateTime start;
    private LocalDateTime end;

    private final List<TelemetryEvent> events = new ArrayList<>();
    private final List<TelemetryObserver> observers = new ArrayList<>();

    public Session(){
        return;
    }

    public synchronized void addObserver(TelemetryObserver observer){
        observers.add(observer);
    }

    public synchronized void removeObserver(TelemetryObserver observer){
        observers.remove(observer);
    }

    private void notifyObservers(TelemetryEvent event){
        for (TelemetryObserver observer: observers){
            observer.update(event);
        }
    }

    private void addEvent(TelemetryEvent event){
        this.events.add(event);

    }
}
