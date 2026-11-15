package com.telemetry.reporting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.telemetry.integration.Session;
import com.telemetry.monitoring.SessionStats;
import com.telemetry.integration.TelemetryEvent;

public class JsonReport implements ReportExporter
{

    @Override
    public void export(Session session, Path output) throws IOException
    {
        SessionStats stats = session.getStats();

        Map<String, Object> report = new LinkedHashMap<>();

        report.put("student_id", session.getStudentId());
        report.put("project", session.getProject());

        report.put("session_start", String.valueOf(session.getStart()));
        report.put("session_end", String.valueOf(session.getEnd()));
        report.put("session_duration_seconds", session.durationSeconds());

        report.put("active_time_seconds", SessionStats.activeSeconds(session));
        report.put("idle_time_seconds", SessionStats.idleSeconds(session));

        report.put(
            "activity_measurement",
            "Estimated using a 180-second inactivity threshold");
        
        report.put("total_compilations", stats.getAttempts());
        report.put("successful_compilations", stats.getSuccesses());
        report.put("failed_compilations", stats.getFailures());
        report.put("success_rate_percent", stats.getSuccessRate());

        report.put("saved_edit_events", stats.getEdits());
        report.put("program_executions", stats.getRuns());
        report.put("runtime_exceptions", stats.getRuntimeErrors());

        report.put("error_frequencies", stats.getErrors());

        report.put(
                "compilation_intervals_seconds",
                stats.getCompileIntervals());

        List<Map<String, String>> entries = new ArrayList<>();

        for(TelemetryEvent event : session.getEvents())
        {
            Map<String, String> item = new LinkedHashMap<>();

            item.put("type", event.getType());
            item.put("timestamp", event.getTime().toString());
            item.put("name", event.getName());
            item.put("details", event.getDetails());

            entries.add(item);
        }

        report.put("events", entries);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Path parent = output.toAbsolutePath().getParent();
        if(parent != null) Files.createDirectories(parent);
        Files.writeString(output, gson.toJson(report), StandardCharsets.UTF_8);
    }

}
