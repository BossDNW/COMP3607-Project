package com.telemetry.monitoring;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import com.telemetry.integration.*;
import com.telemetry.monitoring.*;

import static org.junit.jupiter.api.Assertions.*;

public class SessionStatsTest {

    @Test
    void calculatesCompileSuccessRate(){
        Session session= new Session();
        session.start("Demo");

        session.record("COMPILATION_STARTED", "", "");
        session.record("COMPILATION_SUCCESS", "", "");

        session.record("COMPILATION_STARTED", "", "");
        session.record("COMPILATION_FAILURE", "", "");

        assertEquals(2, session.getStats().getAttempts());
        assertEquals(1, session.getStats().getSuccesses());
        assertEquals(1, session.getStats().getFailures());
        assertEquals(50.0, session.getStats().getSuccessRate());
    }

    @Test
    void computesIntervalsAndErrorFrequency(){
        SessionStats stats= new SessionStats();

        LocalDateTime t= LocalDateTime.of(2026, 11, 14, 9, 0);

        stats.update(new TelemetryEvent("COMPILATION_STARTED", t, "", ""));
        stats.update(new TelemetryEvent("COMPILATION_STARTED", t.plusSeconds(30), "", ""));
        stats.update(new TelemetryEvent("COMPILATION_ERROR", t, "", "Line 4: expected ;"));
        stats.update(new TelemetryEvent("COMPILATION_ERROR", t, "", "Line 4: expected ;"));

        assertEquals(30L, stats.getCompileIntervals().get(0));
        assertEquals(2, stats.getErrors().get("Line 4: expected ;"));

    }

}
