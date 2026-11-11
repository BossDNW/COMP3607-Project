
package com.telemetry.session;

import com.telemetry.events.TelemetryEvent;
import com.telemetry.events.TelemetryRecorder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SessionManager implements TelemetryRecorder {

    private final List<TelemetryEvent> events = new ArrayList<>();

    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    private boolean active;

    public synchronized void startSession() {
        events.clear();
        sessionStart = LocalDateTime.now();
        sessionEnd = null;
        active = true;
    }

    @Override
    public synchronized void recordEvent(TelemetryEvent event) {
        if (!active) {
            return;
        }

        events.add(event);
    }

    public synchronized void endSession() {
        if (!active) {
            return;
        }

        sessionEnd = LocalDateTime.now();
        active = false;
    }

    public synchronized List<TelemetryEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public synchronized Duration getSessionDuration() {
        if (sessionStart == null) {
            return Duration.ZERO;
        }

        LocalDateTime end = active
                ? LocalDateTime.now()
                : sessionEnd;

        return Duration.between(sessionStart, end);
    }

    public synchronized LocalDateTime getSessionStart() {
        return sessionStart;
    }

    public synchronized LocalDateTime getSessionEnd() {
        return sessionEnd;
    }

    public synchronized boolean isActive() {
        return active;
    }


public synchronized SessionManager snapshot() {

    SessionManager copy = new SessionManager();

    copy.events.addAll(this.events);
    copy.sessionStart = this.sessionStart;

    copy.sessionEnd = this.active
            ? LocalDateTime.now()
            : this.sessionEnd;

    copy.active = false;

    return copy;
}

}
