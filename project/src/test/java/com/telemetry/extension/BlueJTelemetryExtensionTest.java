package com.telemetry.extension;

import bluej.extensions2.BClass;
import bluej.extensions2.BPackage;

import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.InvocationFinishedEvent;

import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.session.SessionManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlueJTelemetryExtensionTest {

    @TempDir
    Path tempDir;

    private BlueJTelemetryExtension extension;
    private SessionManager session;

    @BeforeEach
    void setUp() throws Exception {

        extension = new BlueJTelemetryExtension();

        Field field = BlueJTelemetryExtension.class
                .getDeclaredField("sessionManager");

        field.setAccessible(true);

        session = (SessionManager) field.get(extension);

        session.startSession();
    }

    private long count(EventType type) {
        return session.getEvents().stream()
                .filter(e -> e.getType() == type)
                .count();
    }

    private TelemetryEvent lastEvent() {
        List<TelemetryEvent> events = session.getEvents();

        assertFalse(events.isEmpty());

        return events.get(events.size() - 1);
    }

    @SuppressWarnings("unchecked")
    private void registerPackage(String... classNames)
            throws Exception {

        Field field = BlueJTelemetryExtension.class
                .getDeclaredField("knownClasses");

        field.setAccessible(true);

        Map<String, Set<String>> known =
                (Map<String, Set<String>>) field.get(extension);

        Set<String> classes = ConcurrentHashMap.newKeySet();

        for (String name : classNames) {
            classes.add(name);
        }

        known.put(tempDir.toFile().getAbsolutePath(), classes);
    }

    private ClassEvent classEvent(
            String className,
            boolean compiled,
            boolean error) throws Exception {

        BPackage pkg = mock(BPackage.class);

        when(pkg.getDir()).thenReturn(tempDir.toFile());

        BClass bClass = mock(BClass.class);

        when(bClass.getName()).thenReturn(className);

        ClassEvent event = mock(ClassEvent.class);

        when(event.getPackage()).thenReturn(pkg);
        when(event.getBClass()).thenReturn(bClass);

        // lenient because not every callback reads these fields
        lenient().when(event.isClassCompiled()).thenReturn(compiled);
        lenient().when(event.hasError()).thenReturn(error);

        return event;
    }

    @Test
    void recordsCompilationStart() {

        CompileEvent event = mock(CompileEvent.class);

        when(event.isUserGeneratedCompilation()).thenReturn(true);

        extension.compileStarted(event);

        assertEquals(1, count(EventType.COMPILATION_STARTED));
    }

    @Test
    void recordsCompilationSuccessAndFailure() {

        CompileEvent event = mock(CompileEvent.class);

        when(event.isUserGeneratedCompilation()).thenReturn(true);

        extension.compileSucceeded(event);
        extension.compileFailed(event);

        assertEquals(1, count(EventType.COMPILATION_SUCCESS));
        assertEquals(1, count(EventType.COMPILATION_FAILURE));
    }

    @Test
    void ignoresNonUserCompilation() {

        CompileEvent event = mock(CompileEvent.class);

        when(event.isUserGeneratedCompilation()).thenReturn(false);

        extension.compileStarted(event);

        assertEquals(0, count(EventType.COMPILATION_STARTED));
    }

    @Test
    void recordsCompilationErrorDetails() {

        CompileEvent event = mock(CompileEvent.class);

        when(event.isUserGeneratedCompilation()).thenReturn(true);
        when(event.getErrorLineNumber()).thenReturn(19);
        when(event.getErrorMessage()).thenReturn("';' expected");

        extension.compileError(event);

        assertEquals(1, count(EventType.COMPILATION_ERROR));

        assertEquals("19",
                lastEvent().getDetails().get("line"));

        assertEquals("';' expected",
                lastEvent().getDetails().get("message"));
    }

    @Test
    void recordsCompilationWarnings() {

        CompileEvent event = mock(CompileEvent.class);

        when(event.isUserGeneratedCompilation()).thenReturn(true);
        when(event.getErrorLineNumber()).thenReturn(5);
        when(event.getErrorMessage()).thenReturn("Test warning");

        extension.compileWarning(event);

        assertEquals(1, count(EventType.COMPILATION_WARNING));
    }

    @Test
    void recordsNormalProgramExecution() {

        InvocationFinishedEvent event =
                mock(InvocationFinishedEvent.class);

        when(event.getClassName()).thenReturn("Hello");
        when(event.getMethodName()).thenReturn("main");

        when(event.getEventType()).thenReturn(
                InvocationFinishedEvent.EventType.NORMAL_EXIT
        );

        extension.invocationFinished(event);

        assertEquals(1, count(EventType.PROGRAM_EXECUTED));
        assertEquals(0, count(EventType.RUNTIME_ERROR));

        assertEquals("main",
                lastEvent().getDetails().get("method"));
    }

    @Test
    void recordsRuntimeException() {

        InvocationFinishedEvent event =
                mock(InvocationFinishedEvent.class);

        when(event.getClassName()).thenReturn("Hello");
        when(event.getMethodName()).thenReturn("crash");

        when(event.getEventType()).thenReturn(
                InvocationFinishedEvent.EventType.EXCEPTION_EXIT
        );

        extension.invocationFinished(event);

        assertEquals(1, count(EventType.PROGRAM_EXECUTED));
        assertEquals(1, count(EventType.RUNTIME_ERROR));
    }

    @Test
    void recordsNewClass() throws Exception {

        registerPackage();

        ClassEvent event = classEvent("NewClass", false, false);

        extension.classStateChanged(event);

        assertEquals(1, count(EventType.CLASS_CREATED));
    }

    @Test
    void recordsModifiedClass() throws Exception {

        registerPackage("Hello");

        ClassEvent event = classEvent("Hello", false, false);

        extension.classStateChanged(event);

        assertEquals(1, count(EventType.CLASS_MODIFIED));
    }

    @Test
    void ignoresCompiledClassState() throws Exception {

        registerPackage("Hello");

        ClassEvent event = classEvent("Hello", true, false);

        extension.classStateChanged(event);

        assertEquals(0, count(EventType.CLASS_MODIFIED));
    }

    @Test
    void recordsClassDeletion() throws Exception {

        registerPackage("Hello");

        ClassEvent event = classEvent("Hello", false, false);

        extension.classRemoved(event);

        assertEquals(1, count(EventType.CLASS_DELETED));
    }

    @Test
    void recordsClassRename() throws Exception {

        registerPackage("OldClass");

        ClassEvent event = classEvent("NewClass", false, false);

        when(event.getOldName()).thenReturn("OldClass");

        extension.classNameChanged(event);

        assertEquals(1, count(EventType.CLASS_RENAMED));

        assertEquals("OldClass",
                lastEvent().getDetails().get("oldName"));

        assertEquals("NewClass",
                lastEvent().getDetails().get("newName"));
    }
}
