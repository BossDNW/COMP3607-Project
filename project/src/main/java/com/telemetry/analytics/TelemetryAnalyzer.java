package com.telemetry.analytics;

import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TelemetryAnalyzer {

    private final List<TelemetryEvent> events;

    public TelemetryAnalyzer(List<TelemetryEvent> events) {
        this.events = new ArrayList<>(events);
    }

    // Count occurrences of a particular event
    public long countEvents(EventType type) {
        return events.stream()
                .filter(event -> event.getType() == type)
                .count();
    }

    // Total compilation attempts
    public long getTotalCompilations() {
        return countEvents(EventType.COMPILATION_STARTED);
    }

    // Successful compilations
    public long getSuccessfulCompilations() {
        return countEvents(EventType.COMPILATION_SUCCESS);
    }

    // Failed compilations
    public long getFailedCompilations() {
        return countEvents(EventType.COMPILATION_FAILURE);
    }

    // Percentage of successful compilations
    public double getCompilationSuccessRate() {
        long total = getTotalCompilations();

        if (total == 0) {
            return 0.0;
        }

        return (getSuccessfulCompilations() * 100.0) / total;
    }

    // Number of individual compilation errors
    public long getTotalErrors() {
        return countEvents(EventType.COMPILATION_ERROR);
    }

    // Frequency of each error message
    public Map<String, Integer> getErrorFrequencies() {
        Map<String, Integer> frequencies = new LinkedHashMap<>();

        for (TelemetryEvent event : events) {
            if (event.getType() == EventType.COMPILATION_ERROR) {
                String message = event.getDetails()
                        .getOrDefault("message", "Unknown error");

                frequencies.merge(message, 1, Integer::sum);
            }
        }

        return frequencies;
    }

    // Intervals between consecutive compilation attempts
    public List<Long> getCompilationIntervals() {
        List<LocalDateTime> timestamps = new ArrayList<>();

        for (TelemetryEvent event : events) {
            if (event.getType() == EventType.COMPILATION_STARTED) {
                timestamps.add(event.getTimestamp());
            }
        }

        timestamps.sort(LocalDateTime::compareTo);

        List<Long> intervals = new ArrayList<>();

        for (int i = 1; i < timestamps.size(); i++) {
            long seconds = Duration.between(
                    timestamps.get(i - 1),
                    timestamps.get(i)
            ).toSeconds();

            intervals.add(seconds);
        }

        return intervals;
    }

    // Average interval between compilation attempts
    public double getAverageCompilationInterval() {
        List<Long> intervals = getCompilationIntervals();

        if (intervals.isEmpty()) {
            return 0.0;
        }

        return intervals.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
    }

    // Number of detected class creations
    public long getClassesCreated() {
        return countEvents(EventType.CLASS_CREATED);
    }

    // Number of detected class deletions
    public long getClassesDeleted() {
        return countEvents(EventType.CLASS_DELETED);
    }
}
