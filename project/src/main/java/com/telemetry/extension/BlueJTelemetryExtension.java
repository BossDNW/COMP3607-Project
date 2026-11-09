package com.telemetry.extension;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.ExtensionException;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;
import bluej.extensions2.event.CompileListener;
import bluej.extensions2.BClass;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.PackageNotFoundException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.events.TelemetryRecorder;
import com.telemetry.session.SessionManager;
import com.telemetry.reports.PdfReportGenerator;
import java.time.LocalDateTime;
import com.telemetry.reports.PdfReportGenerator;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;

import com.telemetry.reports.JsonExporter;
import java.nio.file.Path;
import java.io.IOException;
import java.time.format.DateTimeFormatter;


public class BlueJTelemetryExtension extends Extension
        implements PackageListener, CompileListener,
                   ClassListener, InvocationFinishedListener {

    
    private final Map<String, Set<String>> knownClasses = new ConcurrentHashMap<>();
    private final SessionManager sessionManager = new SessionManager();
    private final TelemetryRecorder recorder = sessionManager;
    private String currentProjectName;
    private final JsonExporter jsonExporter = new JsonExporter();
    private final PdfReportGenerator pdfGenerator = new PdfReportGenerator();

    private String packageKey(bluej.extensions2.BPackage pkg) {
        try {
            return pkg.getDir().getAbsolutePath();
        } catch (ProjectNotOpenException | PackageNotFoundException e) {
            // BlueJ may send class events before a project finishes opening.
            return null;
        }
    }


    @Override
    public void startup(BlueJ bluej) {
        bluej.addPackageListener(this);
        bluej.addCompileListener(this);
        bluej.addClassListener(this);
        bluej.addInvocationFinishedListener(this);

        System.out.println("BlueJ Telemetry started!");
    }

    
    
    @Override
    public void packageOpened(PackageEvent event) {
        try {
            String name = event.getPackage().getProject().getName();
            String key = packageKey(event.getPackage());

            if (key == null) {
                return;
            }

            Set<String> classes = ConcurrentHashMap.newKeySet();

            for (BClass bClass : event.getPackage().getClasses()) {
                classes.add(bClass.getName());
            }

            knownClasses.put(key, classes);

            if (!sessionManager.isActive()) {
                currentProjectName = name;
                sessionManager.startSession();

                record(EventType.PROJECT_OPENED, null, Map.of());
            }

            log("PROJECT OPENED: " + name);

        } catch (ExtensionException e) {
            log("Unable to identify project: " + e.getMessage());
        }
    }



    
    
    
    @Override
    public void packageClosing(PackageEvent event) {

        if (sessionManager.isActive()) {

            record(EventType.PROJECT_CLOSED, null, Map.of());
            sessionManager.endSession();

            log("SESSION ENDED");
            log("TOTAL EVENTS: "
                    + sessionManager.getEvents().size());

            try {
                String timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                );

                Path output = Path.of(
                    System.getProperty("user.home"),
                    "bluej",
                    "telemetry-reports",
                    "session-" + timestamp + ".json"
                );

                jsonExporter.export(sessionManager, output);

                log("JSON EXPORTED: " + output);

                Path pdfOutput = output.resolveSibling(
                        output.getFileName().toString()
                                .replace(".json", ".pdf")
                );

                pdfGenerator.generate(sessionManager, pdfOutput);

                log("PDF EXPORTED: " + pdfOutput);


            } catch (IOException e) {
                log("JSON EXPORT FAILED: " + e.getMessage());
            }
        }

        knownClasses.clear();
    }



    @Override
    public void compileStarted(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION STARTED");
            record(EventType.COMPILATION_STARTED, null, Map.of());
        }
    }

    @Override
    public void compileSucceeded(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION SUCCESS");
            record(EventType.COMPILATION_SUCCESS, null, Map.of());
        }
    }

    @Override
    public void compileFailed(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION FAILED");
            record(EventType.COMPILATION_FAILURE, null, Map.of());
        }
    }

    @Override
    public void compileError(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION ERROR");
            System.out.println("Line: " + event.getErrorLineNumber());
            System.out.println("Message: " + event.getErrorMessage());
            record(EventType.COMPILATION_ERROR, null, Map.of("line", String.valueOf(event.getErrorLineNumber()),
                "message", String.valueOf(event.getErrorMessage())
            ));
        }
    }

    @Override
    public void compileWarning(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION WARNING");
            System.out.println("Message: " + event.getErrorMessage());
            record(EventType.COMPILATION_WARNING, null, Map.of(
                "line", String.valueOf(event.getErrorLineNumber()),
                "message", String.valueOf(event.getErrorMessage())
            ));
        }
    }

    
    @Override
    public void invocationFinished(InvocationFinishedEvent event) {

        if (!sessionManager.isActive()) {
            return;
        }

        String className = event.getClassName();
        String methodName = event.getMethodName();

        Map<String, String> details = Map.of(
                "method", methodName == null
                        ? "<constructor>" : methodName,
                "status", event.getEventType().name()
        );

        record(EventType.PROGRAM_EXECUTED, className, details);

        log("PROGRAM EXECUTED: " + className
                + " | " + details.get("method")
                + " | " + event.getEventType());

        if (event.getEventType()
                == InvocationFinishedEvent.EventType.EXCEPTION_EXIT) {

            record(EventType.RUNTIME_ERROR, className, details);

            log("RUNTIME EXCEPTION DETECTED: " + className);
        }
    }

    
    
    @Override
    public void classStateChanged(ClassEvent event) {
        String key = packageKey(event.getPackage());

        if (key == null) {
            return;
        }

        Set<String> classes = knownClasses.get(key);

        if (classes == null) {
            return;
        }

        String name = event.getBClass().getName();

        if (classes.add(name)) {
            log("CLASS CREATED: " + name);
            record(EventType.CLASS_CREATED, name, Map.of());
            return;
        }

        if (!event.isClassCompiled() && !event.hasError()) {
            log("CLASS MODIFIED: " + name);
            record(EventType.CLASS_MODIFIED, name, Map.of());
        }
        
    }


    @Override
    public void classNameChanged(ClassEvent event) {
        String key = packageKey(event.getPackage());

        if (key == null) {
            return;
        }

        Set<String> classes = knownClasses.get(key);

        if (classes != null) {
            classes.remove(event.getOldName());
            classes.add(event.getBClass().getName());
        }

        log("CLASS RENAMED: " + event.getOldName() + " -> " + event.getBClass().getName());
        record(EventType.CLASS_RENAMED, event.getBClass().getName(), Map.of("oldName", event.getOldName(), "newName", event.getBClass().getName()));

    }

    @Override
    public void classRemoved(ClassEvent event) {
        String name = event.getBClass().getName();
        String key = packageKey(event.getPackage());

        if (key == null) {
            return;
        }

        Set<String> classes = knownClasses.get(key);

        if (classes != null) {
            classes.remove(name);
        }

        log("CLASS DELETED: " + name);
        record(EventType.CLASS_DELETED, name, Map.of());
    }



    private void log(String message) {
        System.out.println(LocalDateTime.now() + " | " + message);
    }

    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override
    public String getName() {
        return "BlueJ Telemetry";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "Collects telemetry from BlueJ coding sessions.";
    }
    
    private void record(EventType type, String className, Map<String, String> details) {

        TelemetryEvent event = new TelemetryEvent(
            type,
            LocalDateTime.now(),
            currentProjectName,
            className,
            details
        );

        recorder.recordEvent(event);

        log("RECORDED: " + type);
    }

}
