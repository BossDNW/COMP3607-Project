package com.telemetry.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import com.telemetry.integration.*;

import static org.junit.jupiter.api.Assertions.*;

public class EditMonitorTest {
    @TempDir
    Path folder;

    @Test
    void detectsSavedChange() throws Exception {
        Path source= folder.resolve("Hello.java");
        Files.writeString(source, "class Hello {}");

        EditMonitor monitor= new EditMonitor();

        Session session= new Session();
        session.start("Demo");

        monitor.watch(folder);
        monitor.check(session);

        assertEquals(0, session.getStats().getEdits());

        Files.writeString(source, "class Hello { int x; }");

        Files.setLastModifiedTime(source, FileTime.fromMillis(Files.getLastModifiedTime(source).toMillis() + 2000));
        monitor.check(session);
        assertEquals(1, session.getStats().getEdits());

        monitor.check(session);
        assertEquals(1, session.getStats().getEdits());
    }

    @Test
    void followsFilesCreatedAfterPackageOpened() throws Exception {
        EditMonitor monitor= new EditMonitor();

        Session session= new Session();
        session.start("Demo");

        monitor.watch(folder);

        Path source= folder.resolve("New.java");
        Files.writeString(source, "class New {}");
        monitor.check(session);

        Files.writeString(source, "class New { int y; }");

        Files.setLastModifiedTime(source, FileTime.fromMillis(Files.getLastModifiedTimes(source).toMillis() + 2000));

        monitor.check(session);
        assertEquals(1, session.getStats().getEdits());
    }


    @Test
    void ignoresUnrelatedFilesAndUnwantedFolders() throws Exception {
        Path notes = folder.resolve("notes.txt");
        Files.writeString(notes, "one");

        EditMonitor monitor = new EditMonitor();
        Session session = new Session();
        session.start("Demo");

        monitor.watch(folder);

        Files.writeString(notes, "two");
        monitor.check(session);

        monitor.unwatch(folder);
        monitor.check(session);

        assertEquals(0, session.getStats().getEdits());
    }

}
