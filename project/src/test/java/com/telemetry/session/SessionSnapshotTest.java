package com.telemetry.session;

import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SessionSnapshotTest {

    private TelemetryEvent event() {
        return new TelemetryEvent(
                EventType.CLASS_CREATED,
                LocalDateTime.now(),
                "TestProject",
                "Hello",
                Map.of()
        );
    }

    @Test
    void snapshotPreservesEventsAndTime() {

        SessionManager session = new SessionManager();
        session.startSession();
        session.recordEvent(event());

        SessionManager snapshot = session.snapshot();

        assertFalse(snapshot.isActive());
        assertEquals(1, snapshot.getEvents().size());
        assertEquals(
                session.getSessionStart(),
                snapshot.getSessionStart()
        );
        assertNotNull(snapshot.getSessionEnd());
    }

    @Test
    void snapshotIsUnaffectedByNewSession() {

        SessionManager session = new SessionManager();
        session.startSession();
        session.recordEvent(event());

        SessionManager snapshot = session.snapshot();

        session.endSession();
        session.startSession();

        assertEquals(1, snapshot.getEvents().size());
        assertTrue(session.getEvents().isEmpty());
    }
}
