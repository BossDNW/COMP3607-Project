package com.telemetry.session;

import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    private TelemetryEvent event(EventType type) {
        return new TelemetryEvent(
                type,
                LocalDateTime.now(),
                "TestProject",
                null,
                Map.of()
        );
    }

    @Test
    void testSessionStartsCorrectly() {
        SessionManager manager = new SessionManager();

        manager.startSession();

        assertTrue(manager.isActive());
        assertNotNull(manager.getSessionStart());
        assertNull(manager.getSessionEnd());
    }

    @Test
    void testEventsAreRecorded() {
        SessionManager manager = new SessionManager();

        manager.startSession();

        manager.recordEvent(event(EventType.PROJECT_OPENED));
        manager.recordEvent(event(EventType.CLASS_CREATED));

        assertEquals(2, manager.getEvents().size());
    }

    @Test
    void testEventsIgnoredWhenInactive() {
        SessionManager manager = new SessionManager();

        manager.recordEvent(event(EventType.CLASS_CREATED));

        assertTrue(manager.getEvents().isEmpty());
    }

    @Test
    void testSessionEndsCorrectly() {
        SessionManager manager = new SessionManager();

        manager.startSession();
        manager.endSession();

        assertFalse(manager.isActive());
        assertNotNull(manager.getSessionEnd());
        assertFalse(manager.getSessionDuration().isNegative());
    }

    @Test
    void testNewSessionClearsOldEvents() {
        SessionManager manager = new SessionManager();

        manager.startSession();
        manager.recordEvent(event(EventType.CLASS_CREATED));

        manager.endSession();
        manager.startSession();

        assertTrue(manager.getEvents().isEmpty());
        assertTrue(manager.isActive());
    }

    @Test
    void testEventsIgnoredAfterSessionEnds() {
        SessionManager manager = new SessionManager();

        manager.startSession();
        manager.endSession();

        manager.recordEvent(event(EventType.COMPILATION_SUCCESS));

        assertTrue(manager.getEvents().isEmpty());
    }

    @Test
    void testDurationBeforeSessionStarts() {
        SessionManager manager = new SessionManager();

        assertEquals(0, manager.getSessionDuration().toSeconds());
    }
}
