package com.telemetry.integration;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Path;


public class SessionTest {
    
    @Test 
    void recordsEventsAndNotifiesObserver(){
        Session session = new Session();
        AtomicInteger notifications = new AtomicInteger();
        session.addObserver(event -> notifications.incrementAndGet());
        session.start("Demo");
        session.record("Compilation_Starter","","");
        session.finish();

        assertEquals(3,session.getEvents().size());
        assertEquals(3,notifications.get());
        assertFalse(session.isActive());
    }

    @Test 
    void snapshotPreservesEvents(){
        Session session = new Session();
        session.start("Demo");
        session.record("Edit_Activity", "demo.java", "saved");

        Session copy = session.snapshot();

        assertEquals(2,copy.getEvents().size());
        assertEquals("Demo", copy.getProject());
        assertNotNull(copy.getEnd());
    }

    @Test
    void ignoresEventsBeforeStartAndAfterFinish() {
        Session session = new Session();

        session.record("COMPILATION_STARTED", "", "");
        assertEquals(0, session.getEvents().size());

        session.start("Demo");
        session.finish();
        session.record("COMPILATION_STARTED", "", "");

        assertEquals(2, session.getEvents().size());
    }

    @Test
    void snapshotIsIndependentOfLaterEvents() {
        Session session = new Session();
        session.setProjectDirectory(Path.of("demo"));
        session.start("Demo");

        Session oldCopy = session.snapshot();
        session.record("COMPILATION_STARTED", "", "");

        assertEquals(1, oldCopy.getEvents().size());
        assertEquals(2, session.getEvents().size());

        assertEquals(
                Path.of("demo"), oldCopy.getProjectDirectory());
    }

    @Test
    void observerReceivesSessionEvents() {
        Session session = new Session();
        AtomicInteger seen = new AtomicInteger();

        session.addObserver(event -> seen.incrementAndGet());

        session.start("Demo");
        session.record("EDIT_ACTIVITY", "X.java", "saved");
        session.finish();

        assertEquals(3, seen.get());
        assertEquals(1, session.getStats().getEdits());
    }
}
