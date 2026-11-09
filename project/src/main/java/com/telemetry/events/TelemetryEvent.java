package com.telemetry.events;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TelemetryEvent {

    private final EventType type;
    private final LocalDateTime timestamp;
    private final String projectName;
    private final String className;
    private final Map<String, String> details;

    public TelemetryEvent(EventType type,
                          LocalDateTime timestamp,
                          String projectName,
                          String className,
                          Map<String, String> details) {

        this.type = type;
        this.timestamp = timestamp;
        this.projectName = projectName;
        this.className = className;
        this.details = details == null
                ? new HashMap<>()
                : new HashMap<>(details);
    }

    public EventType getType() {
        return type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getDetails() {
        return Collections.unmodifiableMap(details);
    }
}
