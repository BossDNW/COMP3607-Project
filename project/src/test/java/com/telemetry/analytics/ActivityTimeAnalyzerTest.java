package com.telemetry.analytics;

import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ActivityTimeAnalyzerTest {

    private final LocalDateTime start =
            LocalDateTime.of(2026, 11, 9, 12, 0);

    private TelemetryEvent event(long seconds) {
        return new TelemetryEvent(
                EventType.CLASS_MODIFIED,
                start.plusSeconds(seconds),
                "Test",
                "Hello",
                Map.of()
        );
    }

    @Test
    void shortSessionHasNoIdleTime() {
        ActivityTimeAnalyzer analyzer =
                new ActivityTimeAnalyzer(List.of(event(30)));

        assertEquals(0,
                analyzer.getIdleTimeSeconds(start, start.plusSeconds(90)));
        assertEquals(90,
                analyzer.getActiveTimeSeconds(start, start.plusSeconds(90)));
    }

    @Test
    void longGapProducesIdleTime() {
        ActivityTimeAnalyzer analyzer =
                new ActivityTimeAnalyzer(List.of(event(300)));

        assertEquals(120,
                analyzer.getIdleTimeSeconds(start, start.plusSeconds(360)));
        assertEquals(240,
                analyzer.getActiveTimeSeconds(start, start.plusSeconds(360)));
    }

    @Test
    void exactly180SecondsIsNotIdle() {
        ActivityTimeAnalyzer analyzer =
                new ActivityTimeAnalyzer(List.of(event(180)));

        assertEquals(0,
                analyzer.getIdleTimeSeconds(start, start.plusSeconds(180)));
    }

    @Test
    void invalidSessionHasZeroDuration() {
        ActivityTimeAnalyzer analyzer =
                new ActivityTimeAnalyzer(List.of());

        assertEquals(0, analyzer.getIdleTimeSeconds(start, start));
        assertEquals(0, analyzer.getActiveTimeSeconds(start, start));
    }
}
