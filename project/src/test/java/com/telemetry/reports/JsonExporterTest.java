package com.telemetry.reports;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.session.SessionManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class JsonExporterTest {

    @TempDir
    Path tempDir;

    private TelemetryEvent event(
            EventType type,
            Map<String, String> details) {

        return new TelemetryEvent(
                type,
                LocalDateTime.now(),
                "TestProject",
                "HelloWorld",
                details
        );
    }

    private SessionManager createSession() {
        SessionManager session = new SessionManager();
        session.startSession();

        session.recordEvent(event(
                EventType.PROJECT_OPENED, Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_STARTED, Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_SUCCESS, Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_STARTED, Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_ERROR,
                Map.of("line", "12",
                       "message", "';' expected")));

        session.recordEvent(event(
                EventType.COMPILATION_FAILURE, Map.of()));

        session.endSession();
        return session;
    }

    @Test
    void testJsonFileCreated() throws Exception {
        SessionManager session = createSession();
        Path output = tempDir.resolve("session.json");

        new JsonExporter().export(session, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    void testCompilationStatistics() throws Exception {
        SessionManager session = createSession();
        Path output = tempDir.resolve("stats.json");

        new JsonExporter().export(session, output);

        JsonObject json = JsonParser.parseString(
                Files.readString(output)
        ).getAsJsonObject();

        assertEquals(2,
                json.get("total_compilations").getAsInt());

        assertEquals(1,
                json.get("successful_compilations").getAsInt());

        assertEquals(1,
                json.get("failed_compilations").getAsInt());

        assertEquals(1,
                json.get("compilation_errors").getAsInt());

        assertEquals(50.0,
                json.get("compilation_success_rate").getAsDouble(),
                0.001);

        assertTrue(json.has("active_time_seconds"));
        assertTrue(json.has("idle_time_seconds"));
        assertTrue(json.has("error_frequencies"));
    }

    @Test
    void testEventDetailsAndSessionMetadata() throws Exception {
        SessionManager session = createSession();
        Path output = tempDir.resolve("events.json");

        new JsonExporter().export(session, output);

        JsonObject json = JsonParser.parseString(
                Files.readString(output)
        ).getAsJsonObject();

        assertEquals("STUDENT_ID",
                json.get("student_id").getAsString());

        assertTrue(json.has("session_start"));
        assertTrue(json.has("session_end"));

        assertEquals(6,
                json.getAsJsonArray("events").size());

        JsonObject error = json.getAsJsonArray("events")
                .get(4).getAsJsonObject();

        assertEquals("COMPILATION_ERROR",
                error.get("type").getAsString());

        assertEquals("12",
                error.getAsJsonObject("details")
                        .get("line").getAsString());

        assertEquals("';' expected",
                error.getAsJsonObject("details")
                        .get("message").getAsString());
    }

    @Test
    void testEmptySessionExport() throws Exception {
        SessionManager session = new SessionManager();
        session.startSession();
        session.endSession();

        Path output = tempDir.resolve("empty.json");

        new JsonExporter().export(session, output);

        JsonObject json = JsonParser.parseString(
                Files.readString(output)
        ).getAsJsonObject();

        assertEquals(0,
                json.get("total_compilations").getAsInt());

        assertEquals(0,
                json.getAsJsonArray("events").size());

        assertEquals(0.0,
                json.get("compilation_success_rate").getAsDouble());
    }
}
