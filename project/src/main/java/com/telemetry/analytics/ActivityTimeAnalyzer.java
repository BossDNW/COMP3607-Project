package com.telemetry.analytics;

import com.telemetry.events.TelemetryEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActivityTimeAnalyzer {

    private static final long IDLE_THRESHOLD_SECONDS = 180;

    private final List<TelemetryEvent> events;

    public ActivityTimeAnalyzer(List<TelemetryEvent> events) {
        this.events = new ArrayList<>(events);
        this.events.sort(
                Comparator.comparing(TelemetryEvent::getTimestamp)
        );
    }

    public long getIdleTimeSeconds(
            LocalDateTime sessionStart,
            LocalDateTime sessionEnd) {

        if (sessionStart == null || sessionEnd == null
                || !sessionEnd.isAfter(sessionStart)) {
            return 0;
        }

        long idleTime = 0;
        LocalDateTime previous = sessionStart;

        for (TelemetryEvent event : events) {

            LocalDateTime current = event.getTimestamp();

            if (current.isBefore(previous)
                    || current.isAfter(sessionEnd)) {
                continue;
            }

            long gap = Duration.between(
                    previous, current
            ).getSeconds();

            if (gap > IDLE_THRESHOLD_SECONDS) {
                idleTime += gap - IDLE_THRESHOLD_SECONDS;
            }

            previous = current;
        }

        long finalGap = Duration.between(
                previous, sessionEnd
        ).getSeconds();

        if (finalGap > IDLE_THRESHOLD_SECONDS) {
            idleTime += finalGap - IDLE_THRESHOLD_SECONDS;
        }

        return idleTime;
    }

    public long getActiveTimeSeconds(
            LocalDateTime sessionStart,
            LocalDateTime sessionEnd) {

        if (sessionStart == null || sessionEnd == null
                || !sessionEnd.isAfter(sessionStart)) {
            return 0;
        }

        long total = Duration.between(
                sessionStart, sessionEnd
        ).getSeconds();

        return total - getIdleTimeSeconds(
                sessionStart, sessionEnd
        );
    }
}
