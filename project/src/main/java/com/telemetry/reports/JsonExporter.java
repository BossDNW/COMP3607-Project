
package com.telemetry.reports;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.telemetry.analytics.ActivityTimeAnalyzer;
import com.telemetry.analytics.TelemetryAnalyzer;
import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.session.SessionManager;
import java.util.prefs.Preferences;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsonExporter implements ReportExporter {

    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    
        private static final Preferences SETTINGS =
                Preferences.userNodeForPackage(JsonExporter.class);

        public static String studentId() {
        String id = SETTINGS.get("student_id", "");

        if (id.isBlank()) {
                id = System.getProperty("telemetry.student.id", "");
        }

        if (id.isBlank()) {
                id = System.getenv("TELEMETRY_STUDENT_ID");
        }

        return id == null || id.isBlank()
                ? "STUDENT_ID"
                : id.trim();
        }

        public static void setStudentId(String id) {
        if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                        "Student ID cannot be blank.");
        }

        SETTINGS.put("student_id", id.trim());
        }


    @Override
    public void export(SessionManager session, Path output)
            throws IOException {

        List<TelemetryEvent> events = session.getEvents();
        TelemetryAnalyzer analyzer = new TelemetryAnalyzer(events);
        ActivityTimeAnalyzer activity =
                new ActivityTimeAnalyzer(events);

        JsonObject report = new JsonObject();

        report.addProperty("student_id", studentId());
        report.addProperty("session_start",
                String.valueOf(session.getSessionStart()));
        report.addProperty("session_end",
                String.valueOf(session.getSessionEnd()));
        report.addProperty("session_duration_seconds",
                session.getSessionDuration().toSeconds());

        report.addProperty("active_time_seconds",
                activity.getActiveTimeSeconds(
                        session.getSessionStart(),
                        session.getSessionEnd()));

        report.addProperty("idle_time_seconds",
                activity.getIdleTimeSeconds(
                        session.getSessionStart(),
                        session.getSessionEnd()));

        report.addProperty("activity_measurement",
                "Estimated from recorded events using a 180-second threshold");

        // Existing compilation fields
        report.addProperty("total_compilations",
                count(events, EventType.COMPILATION_STARTED));
        report.addProperty("successful_compilations",
                count(events, EventType.COMPILATION_SUCCESS));
        report.addProperty("failed_compilations",
                count(events, EventType.COMPILATION_FAILURE));
        report.addProperty("compilation_errors",
                count(events, EventType.COMPILATION_ERROR));
        report.addProperty("compilation_success_rate",
                analyzer.getCompilationSuccessRate());
        report.addProperty("average_compilation_interval_seconds",
                analyzer.getAverageCompilationInterval());

        // Existing class fields
        report.addProperty("classes_created",
                analyzer.getClassesCreated());
        report.addProperty("classes_deleted",
                analyzer.getClassesDeleted());

        // Extended class, editing, and execution summaries
        report.addProperty("classes_modified",
                count(events, EventType.CLASS_MODIFIED));
        report.addProperty("classes_renamed",
                count(events, EventType.CLASS_RENAMED));
        report.addProperty("saved_edit_events",
                count(events, EventType.EDIT_ACTIVITY));
        report.addProperty("program_executions",
                count(events, EventType.PROGRAM_EXECUTED));
        report.addProperty("runtime_exceptions",
                count(events, EventType.RUNTIME_ERROR));
        report.addProperty("compilation_warnings",
                count(events, EventType.COMPILATION_WARNING));

        JsonArray intervals = new JsonArray();
        for (Long interval : analyzer.getCompilationIntervals()) {
            intervals.add(interval);
        }
        report.add("compilation_intervals_seconds", intervals);

        JsonObject errorFrequencies = new JsonObject();
        for (Map.Entry<String, Integer> entry
                : analyzer.getErrorFrequencies().entrySet()) {
            errorFrequencies.addProperty(
                    entry.getKey(), entry.getValue());
        }
        report.add("error_frequencies", errorFrequencies);

        JsonObject eventCounts = new JsonObject();
        for (EventType type : EventType.values()) {
            eventCounts.addProperty(type.name(),
                    count(events, type));
        }
        report.add("event_counts", eventCounts);

        JsonArray runtimeEvents = new JsonArray();
        JsonArray editEvents = new JsonArray();
        JsonArray eventArray = new JsonArray();

        for (TelemetryEvent event : events) {
            JsonObject item = toJson(event);
            eventArray.add(item);

            if (event.getType() == EventType.PROGRAM_EXECUTED
                    || event.getType() == EventType.RUNTIME_ERROR) {
                runtimeEvents.add(item.deepCopy());
            }

            if (event.getType() == EventType.EDIT_ACTIVITY) {
                editEvents.add(item.deepCopy());
            }
        }

        report.add("runtime_events", runtimeEvents);
        report.add("edit_events", editEvents);
        report.add("events", eventArray);

        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Files.writeString(output,
                gson.toJson(report), StandardCharsets.UTF_8);
    }

    private JsonObject toJson(TelemetryEvent event) {
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
                    entry.getKey(), entry.getValue());
        }

        item.add("details", details);
        return item;
    }

    private long count(List<TelemetryEvent> events,
                       EventType type) {
        return events.stream()
                .filter(e -> e.getType() == type)
                .count();
    }
}
