
package com.telemetry.integration;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.BProject;

import bluej.extensions2.event.PackageEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests BlueJ extension registration and session lifecycle.
 * These tests use Mockito instead of launching BlueJ.
 */
class BlueJExtensionLifecycleTest {

    @TempDir
    Path folder;

    @Test
    void startupRegistersBlueJListeners() {
        BlueJ bluej = mock(BlueJ.class);
        BlueJTelemetryExtension extension =
                new BlueJTelemetryExtension();

        try {
            extension.startup(bluej);

            verify(bluej).addPackageListener(extension);
            verify(bluej).addClassListener(extension);
            verify(bluej).addCompileListener(extension);
            verify(bluej).addInvocationFinishedListener(extension);

            verify(bluej).setMenuGenerator(any());
        } finally {
            extension.terminate();
        }
    }

    @Test
    void openingPackageStartsSession() throws Exception {
        BlueJTelemetryExtension extension =
                new BlueJTelemetryExtension();

        BPackage pkg = mock(BPackage.class);
        BProject project = mock(BProject.class);
        BClass bluejClass = mock(BClass.class);

        when(pkg.getDir()).thenReturn(folder.toFile());
        when(pkg.getProject()).thenReturn(project);
        when(pkg.getClasses()).thenReturn(
                new BClass[]{bluejClass});

        when(project.getName()).thenReturn("DemoProject");
        when(project.getDir()).thenReturn(folder.toFile());

        when(bluejClass.getName()).thenReturn("Hello");

        PackageEvent event = mock(PackageEvent.class);
        when(event.getPackage()).thenReturn(pkg);

        extension.packageOpened(event);

        Field sessionField =
                BlueJTelemetryExtension.class
                        .getDeclaredField("session");

        sessionField.setAccessible(true);

        Session session =
                (Session) sessionField.get(extension);

        assertNotNull(session);
        assertTrue(session.isActive());

        assertEquals("DemoProject", session.getProject());
        assertEquals(folder, session.getProjectDirectory());

        assertEquals(1, session.getEvents().size());
        assertEquals("Project_Opened",
                session.getEvents().get(0).getType());
    }

    @Test
    void extensionReturnsCorrectMetadata() {
        BlueJTelemetryExtension extension =
                new BlueJTelemetryExtension();

        assertEquals("BlueJ Telemetry", extension.getName());
        assertEquals("2.0", extension.getVersion());

        assertEquals(
                "Simple BlueJ coding session tracker",
                extension.getDescription());
    }
}
