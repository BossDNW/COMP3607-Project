package com.telemetry.integration;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.telemetry.monitoring.SessionStats;

public class Session {
    private String project;
    private String studentId = "STUDENT_ID";
    private Path projectDirectory;
    private LocalDateTime start;
    private LocalDateTime end;

    private final List<TelemetryEvent> events = new ArrayList<>();
    private final List<TelemetryObserver> observers = new ArrayList<>();
    private final SessionStats stats = new SessionStats();

    public Session(){
        return;
    }

    public synchronized void addObserver(TelemetryObserver observer){
        this.observers.add(observer);
    }

    public synchronized void removeObserver(TelemetryObserver observer){
        this.observers.remove(observer);
    }

    private void notifyObservers(TelemetryEvent event){
        for (TelemetryObserver observer: this.observers){
            observer.update(event);
        }
    }

    private void addEvent(TelemetryEvent event){
        this.events.add(event);
        notifyObservers(event);
    }

    public synchronized void record(String type, String name, String details){
        if (start == null || end != null){
            return;
        }

        addEvent(new TelemetryEvent(type, LocalDateTime.now(), name, details));
    }

    public synchronized void start(String projectName){
        this.project = projectName;
        this.start = LocalDateTime.now();
        this.end = null;
        events.clear();
        record("Project_Opened", projectName, "");
    }

    public synchronized boolean isActive(){
        return this.start != null && this.end == null;
    }

    public synchronized void finish(){
        if (isActive()){
            record("Project_Closed",project,"");
            this.end = LocalDateTime.now();
        }
    }

    public synchronized Session snapshot(){
        Session copy = new Session();

        copy.project = this.project;
        copy.studentId = this.studentId;
        copy.projectDirectory = this.projectDirectory;
        copy.start = this.start;
        copy.end = end == null ? LocalDateTime.now(): end;

        for (TelemetryEvent event : events){
            copy.addEvent(event);
        }

        return copy;
    }

    public synchronized String getProject(){
        return this.project;
    }

    public synchronized Path getProjectDirectory(){
        return this.projectDirectory;
    }

    public synchronized void setProjectDirectory(Path dir){
        this.projectDirectory = dir;
    }

    public synchronized String getStudentId(){
        return this.studentId;
    }

    public synchronized void setStudentId(String id){
        this.studentId = id;
    }

    public synchronized LocalDateTime getStart() {
        return start;
    }

    public synchronized LocalDateTime getEnd() {
        return end;
    }

    public synchronized List<TelemetryEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public synchronized SessionStats getStats() {
        return stats;
    }

    public synchronized long durationSeconds() {
        if (start == null) {
            return 0;
        }

        LocalDateTime stop = end == null
                ? LocalDateTime.now()
                : end;

        return Math.max(
                0, Duration.between(start, stop).getSeconds());
    }
}
