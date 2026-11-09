package com.telemetry.analytics;

import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryAnalyzerTest {

    private final LocalDateTime start =
            LocalDateTime.of(2026, 11, 9, 12, 0);

    private TelemetryEvent event(
            EventType type,
            LocalDateTime time,
            Map<String, String> details) {

        return new TelemetryEvent(
                type,
                time,
                "TestProject",
                "HelloWorld",
                details
        );
    }

    @Test
    void testCompilationStatistics() {
        List<TelemetryEvent> events = List.of(
            event(EventType.COMPILATION_STARTED, start, Map.of()),
            event(EventType.COMPILATION_SUCCESS, start.plusSeconds(1), Map.of()),

            event(EventType.COMPILATION_STARTED, start.plusSeconds(30), Map.of()),
            event(EventType.COMPILATION_FAILURE, start.plusSeconds(31), Map.of()),

            event(EventType.COMPILATION_STARTED, start.plusSeconds(120), Map.of()),
            event(EventType.COMPILATION_SUCCESS, start.plusSeconds(121), Map.of())
        );

        TelemetryAnalyzer analyzer = new TelemetryAnalyzer(events);

        assertEquals(3, analyzer.getTotalCompilations());
        assertEquals(2, analyzer.getSuccessfulCompilations());
        assertEquals(1, analyzer.getFailedCompilations());
        assertEquals(66.67, analyzer.getCompilationSuccessRate(), 0.01);
    }

    @Test
    void testCompilationIntervals() {
        List<TelemetryEvent> events = List.of(
            event(EventType.COMPILATION_STARTED, start, Map.of()),
            event(EventType.COMPILATION_STARTED, start.plusSeconds(30), Map.of()),
            event(EventType.COMPILATION_STARTED, start.plusSeconds(120), Map.of())
        );

        TelemetryAnalyzer analyzer = new TelemetryAnalyzer(events);

        assertEquals(
            List.of(30L, 90L),
            analyzer.getCompilationIntervals()
        );

        assertEquals(
            60.0,
            analyzer.getAverageCompilationInterval(),
            0.001
        );
    }

    @Test
    void testErrorFrequencies() {
        List<TelemetryEvent> events = List.of(
            event(EventType.COMPILATION_ERROR, start,
                    Map.of("message", "';' expected")),

            event(EventType.COMPILATION_ERROR, start.plusSeconds(2),
                    Map.of("message", "';' expected")),

            event(EventType.COMPILATION_ERROR, start.plusSeconds(5),
                    Map.of("message", "cannot find symbol"))
        );

        TelemetryAnalyzer analyzer = new TelemetryAnalyzer(events);

        assertEquals(3, analyzer.getTotalErrors());
        assertEquals(2, analyzer.getErrorFrequencies().get("';' expected"));
        assertEquals(1, analyzer.getErrorFrequencies().get("cannot find symbol"));
    }

    @Test
    void testNoCompilations() {
        TelemetryAnalyzer analyzer = new TelemetryAnalyzer(List.of());

        assertEquals(0, analyzer.getTotalCompilations());
        assertEquals(0.0, analyzer.getCompilationSuccessRate());
        assertTrue(analyzer.getCompilationIntervals().isEmpty());
    }

    @Test
    void testClassActivity() {
        List<TelemetryEvent> events = List.of(
            event(EventType.CLASS_CREATED, start, Map.of()),
            event(EventType.CLASS_CREATED, start.plusSeconds(10), Map.of()),
            event(EventType.CLASS_DELETED, start.plusSeconds(20), Map.of())
        );

        TelemetryAnalyzer analyzer = new TelemetryAnalyzer(events);

        assertEquals(2, analyzer.getClassesCreated());
        assertEquals(1, analyzer.getClassesDeleted());
    }
}

