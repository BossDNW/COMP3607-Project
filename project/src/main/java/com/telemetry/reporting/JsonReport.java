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

public class JsonReport implements ReportExporter
{

    @Override
    public void export(Session session, Path output) throws IOException
    {
        SessionStats stats = new session.getStats();

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
        

    }

}
