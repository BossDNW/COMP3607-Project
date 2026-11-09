package com.telemetry.reports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.telemetry.analytics.TelemetryAnalyzer;
import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.analytics.ActivityTimeAnalyzer;
import com.telemetry.session.SessionManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsonExporter {
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public void export(SessionManager session, Path output)
            throws IOException {

        List<TelemetryEvent> events = session.getEvents();
        TelemetryAnalyzer analyzer = new TelemetryAnalyzer(events);

        ActivityTimeAnalyzer activityAnalyzer = new ActivityTimeAnalyzer(events);


        JsonObject report = new JsonObject();

        // Session metadata
        report.addProperty("student_id", "STUDENT_ID");
        report.addProperty("session_start",
                String.valueOf(session.getSessionStart()));
        report.addProperty("session_end",
                String.valueOf(session.getSessionEnd()));
        report.addProperty("session_duration_seconds",
                session.getSessionDuration().toSeconds());
        
        report.addProperty(
                "active_time_seconds",
                activityAnalyzer.getActiveTimeSeconds(
                        session.getSessionStart(),
                        session.getSessionEnd()
                )
        );

        report.addProperty(
                "idle_time_seconds",
                activityAnalyzer.getIdleTimeSeconds(
                        session.getSessionStart(),
                        session.getSessionEnd()
                )
        );

        report.addProperty(
                "activity_measurement",
                "Estimated from recorded events using a 180-second threshold"
        );


        // Compilation statistics
        report.addProperty("total_compilations",
                count(events, EventType.COMPILATION_STARTED));

        report.addProperty("successful_compilations",
                count(events, EventType.COMPILATION_SUCCESS));

        report.addProperty("failed_compilations",
                count(events, EventType.COMPILATION_FAILURE));

        report.addProperty("compilation_errors",
                count(events, EventType.COMPILATION_ERROR));

        
        report.addProperty(
            "compilation_success_rate",
            analyzer.getCompilationSuccessRate()
        );

        report.addProperty(
            "average_compilation_interval_seconds",
            analyzer.getAverageCompilationInterval()
        );

        report.addProperty(
            "classes_created",
            analyzer.getClassesCreated()
        );

        report.addProperty(
            "classes_deleted",
            analyzer.getClassesDeleted()
        );

        // Individual intervals between compilations
        JsonArray intervals = new JsonArray();

        for (Long interval : analyzer.getCompilationIntervals()) {
            intervals.add(interval);
        }

        report.add("compilation_intervals_seconds", intervals);

        // Group errors by their messages
        JsonObject errorFrequencies = new JsonObject();

        for (Map.Entry<String, Integer> entry
                : analyzer.getErrorFrequencies().entrySet()) {

            errorFrequencies.addProperty(
                entry.getKey(),
                entry.getValue()
            );
        }

        report.add("error_frequencies", errorFrequencies);


        // Full event history
        JsonArray eventArray = new JsonArray();

        for (TelemetryEvent event : events) {
            JsonObject item = new JsonObject();

            item.addProperty("type", event.getType().name());
            item.addProperty("timestamp",
                    event.getTimestamp().toString());

            item.addProperty("project_name",
                    event.getProjectName());

            item.addProperty("class_name",
                    event.getClassName());

            JsonObject details = new JsonObject();

            for (Map.Entry<String, String> entry
                    : event.getDetails().entrySet()) {
                details.addProperty(
                        entry.getKey(),
                        entry.getValue()
                );
            }

            item.add("details", details);
            eventArray.add(item);
        }

        report.add("events", eventArray);

        Path parent = output.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(
                output,
                gson.toJson(report),
                StandardCharsets.UTF_8
        );
    }

    private long count(List<TelemetryEvent> events,
                       EventType type) {

        return events.stream()
                .filter(event -> event.getType() == type)
                .count();
    }

    
}
