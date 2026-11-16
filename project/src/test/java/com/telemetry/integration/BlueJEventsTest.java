
package com.telemetry.integration;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.InvocationFinishedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tests BlueJ callbacks using simulated events. */
class BlueJEventsTest {

    @TempDir
    Path folder;

    private BlueJTelemetryExtension extension;
    private Session session;

    @BeforeEach
    void setup() throws Exception {
        extension = new BlueJTelemetryExtension();

        session = new Session();
        session.start("Demo");

        Field field = BlueJTelemetryExtension.class
                .getDeclaredField("session");

        field.setAccessible(true);
        field.set(extension, session);
    }

    private long count(String type) {
        return session.getEvents().stream()
                .filter(event -> event.getType().equals(type))
                .count();
    }

    @Test
    void compileStartAndSuccessAreRecorded() {
        CompileEvent event = mock(CompileEvent.class);
        when(event.isUserGeneratedCompilation()).thenReturn(true);

        extension.compileStarted(event);
        extension.compileSucceeded(event);

        assertEquals(1, count("COMPILATION_STARTED"));
        assertEquals(1, count("COMPILATION_SUCCESS"));
        assertEquals(100.0, session.getStats().getSuccessRate());
    }

    @Test
    void failedCompilationAndDiagnosticAreRecorded() {
        CompileEvent event = mock(CompileEvent.class);

        when(event.isUserGeneratedCompilation()).thenReturn(true);
        when(event.getErrorLineNumber()).thenReturn(12);
        when(event.getErrorMessage()).thenReturn("';' expected");

        extension.compileStarted(event);
        extension.compileError(event);
        extension.compileFailed(event);

        assertEquals(1, count("COMPILATION_ERROR"));
        assertEquals(1, count("COMPILATION_FAILURE"));

        assertTrue(session.getStats().getErrors()
                .containsKey("Line 12: ';' expected"));
    }

    @Test
    void warningIsRecorded() {
        CompileEvent event = mock(CompileEvent.class);

        when(event.isUserGeneratedCompilation()).thenReturn(true);
        when(event.getErrorMessage()).thenReturn("warning");

        extension.compileWarning(event);

        assertEquals(1, count("COMPILATION_WARNING"));
    }

    @Test
    void bluejInternalCompilationIsIgnored() {
        CompileEvent event = mock(CompileEvent.class);

        extension.compileStarted(event);
        extension.compileSucceeded(event);
        extension.compileFailed(event);
        extension.compileError(event);
        extension.compileWarning(event);

        assertEquals(1, session.getEvents().size());
    }

    @Test
    void normalExecutionIsRecorded() {
        InvocationFinishedEvent event =
                mock(InvocationFinishedEvent.class);

        when(event.getClassName()).thenReturn("Calculator");
        when(event.getMethodName()).thenReturn("add");
        when(event.getEventType()).thenReturn(
                InvocationFinishedEvent.EventType.NORMAL_EXIT);

        extension.invocationFinished(event);

        assertEquals(1, count("PROGRAM_EXECUTED"));
        assertEquals(0, count("RUNTIME_ERROR"));
    }

    @Test
    void runtimeExceptionIsRecorded() {
        InvocationFinishedEvent event =
                mock(InvocationFinishedEvent.class);

        when(event.getClassName()).thenReturn("Calculator");
        when(event.getEventType()).thenReturn(
                InvocationFinishedEvent.EventType.EXCEPTION_EXIT);

        extension.invocationFinished(event);

        assertEquals(1, count("PROGRAM_EXECUTED"));
        assertEquals(1, count("RUNTIME_ERROR"));
    }

    private ClassEvent classEvent(String name) throws Exception {
        BPackage pkg = mock(BPackage.class);
        when(pkg.getDir()).thenReturn(folder.toFile());

        BClass bluejClass = mock(BClass.class);
        when(bluejClass.getName()).thenReturn(name);

        ClassEvent event = mock(ClassEvent.class);
        when(event.getPackage()).thenReturn(pkg);
        when(event.getBClass()).thenReturn(bluejClass);

        return event;
    }

    @SuppressWarnings("unchecked")
    private void known(String... names) throws Exception {
        Field field = BlueJTelemetryExtension.class
                .getDeclaredField("knownClasses");

        field.setAccessible(true);

        Map<String, Set<String>> map =
                (Map<String, Set<String>>) field.get(extension);

        Set<String> set = new HashSet<>();

        for (String name : names) {
            set.add(name);
        }

        map.put(folder.toFile().getAbsolutePath(), set);
    }

    @Test
    void classCreatedIsRecorded() throws Exception {
        known();
        extension.classStateChanged(classEvent("Hello"));

        assertEquals(1, count("CLASS_CREATED"));
    }

    @Test
    void modifiedClassIsRecorded() throws Exception {
        known("Hello");
        extension.classStateChanged(classEvent("Hello"));

        assertEquals(1, count("CLASS_MODIFIED"));
    }

    @Test
    void compiledClassDoesNotCountAsSavedEdit()
            throws Exception {

        known("Hello");

        ClassEvent event = classEvent("Hello");
        when(event.isClassCompiled()).thenReturn(true);

        extension.classStateChanged(event);

        assertEquals(0, count("CLASS_MODIFIED"));
    }

    @Test
    void removedClassIsRecorded() throws Exception {
        known("Hello");
        extension.classRemoved(classEvent("Hello"));

        assertEquals(1, count("CLASS_DELETED"));
    }

    @Test
    void renamedClassIsRecorded() throws Exception {
        known("Old");

        ClassEvent event = classEvent("New");
        when(event.getOldName()).thenReturn("Old");

        extension.classNameChanged(event);

        assertEquals(1, count("CLASS_RENAMED"));
    }
}
