
package com.telemetry.extension;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.ExtensionException;
import bluej.extensions2.BClass;
import bluej.extensions2.ProjectNotOpenException;
import bluej.extensions2.PackageNotFoundException;
import com.telemetry.reports.ReportService;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.PackageListener;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.ClassListener;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;
import bluej.extensions2.MenuGenerator;
import bluej.extensions2.BPackage;
import javafx.scene.control.MenuItem;
import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.events.TelemetryRecorder;
import com.telemetry.events.TelemetryEventBus;
import com.telemetry.reports.JsonExporter;
import javafx.scene.control.Menu;
import javafx.scene.control.TextInputDialog;
import com.telemetry.session.SessionManager;

import com.telemetry.reports.ReportExporterCreator;
import com.telemetry.reports.JsonExporterCreator;
import com.telemetry.reports.PdfExporterCreator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlueJTelemetryExtension extends Extension
        implements PackageListener, CompileListener,
                   ClassListener, InvocationFinishedListener {

    private final Map<String, Set<String>> knownClasses =
            new ConcurrentHashMap<>();

    private final SessionManager sessionManager =
            new SessionManager();

    private final TelemetryRecorder recorder = sessionManager;

    private final TelemetryEventBus eventBus =
            new TelemetryEventBus();

    private final ReportService reportService = new ReportService();

    private String currentProjectName;

    private final Map<Path, Map<Path, Long>> sourceFileTimes =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService editMonitor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "bluej-edit-monitor");
                thread.setDaemon(true);
                return thread;
            });

    private final ReportExporterCreator jsonCreator =
            new JsonExporterCreator();

    private final ReportExporterCreator pdfCreator =
            new PdfExporterCreator();

    public BlueJTelemetryExtension() {
        eventBus.subscribe(recorder);
    }

    private String packageKey(bluej.extensions2.BPackage pkg) {
        try {
            return pkg.getDir().getAbsolutePath();
        } catch (ProjectNotOpenException | PackageNotFoundException e) {
            return null;
        }
    }

    @Override
    public void startup(BlueJ bluej) {

        bluej.addPackageListener(this);
        bluej.addCompileListener(this);
        bluej.addClassListener(this);
        bluej.addInvocationFinishedListener(this);

        reportService.startAutomaticBackups(sessionManager);

        editMonitor.scheduleAtFixedRate(this::checkSavedEdits,
                5, 5, TimeUnit.SECONDS);

        
        bluej.setMenuGenerator(new MenuGenerator() {

            @Override
            public MenuItem getToolsMenuItem(BPackage pkg) {

                Menu menu = new Menu("BlueJ Telemetry");

                MenuItem exportItem =
                        new MenuItem("Export Telemetry Reports");

                exportItem.setDisable(!sessionManager.isActive());

                exportItem.setOnAction(e -> {
                    if (!sessionManager.isActive()) {
                        log("No active session to export.");
                        return;
                    }

                    reportService.exportReports(sessionManager);
                    log("MANUAL REPORT EXPORT REQUESTED");
                });

                MenuItem studentIdItem =
                        new MenuItem("Set Student ID");

                studentIdItem.setOnAction(e -> {
                    TextInputDialog dialog =
                            new TextInputDialog(JsonExporter.studentId());

                    dialog.setTitle("BlueJ Telemetry");
                    dialog.setHeaderText("Student Identification");
                    dialog.setContentText("Enter your student ID:");

                    dialog.showAndWait().ifPresent(id -> {
                        if (id.isBlank()) {
                            log("Student ID was not changed.");
                            return;
                        }

                        JsonExporter.setStudentId(id);
                        log("Student ID updated.");
                    });
                });

                menu.getItems().addAll(exportItem, studentIdItem);
                return menu;
            }

            @Override
            public void notifyPostToolsMenu(
                    BPackage pkg,
                    MenuItem item) {

                if (item instanceof Menu menu) {
                    for (MenuItem child : menu.getItems()) {
                        if ("Export Telemetry Reports"
                                .equals(child.getText())) {
                            child.setDisable(
                                    !sessionManager.isActive());
                        }
                    }
                }
            }
        });


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
            registerSourceDirectory(event.getPackage().getDir().toPath());

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

            reportService.exportReports(sessionManager);

            log("REPORT EXPORT REQUESTED");
        }

        knownClasses.clear();
        sourceFileTimes.clear();
    }

    @Override
    public void compileStarted(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION STARTED");

            record(
                    EventType.COMPILATION_STARTED,
                    null,
                    Map.of()
            );
        }
    }

    @Override
    public void compileSucceeded(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION SUCCESS");

            record(
                    EventType.COMPILATION_SUCCESS,
                    null,
                    Map.of()
            );
        }
    }

    @Override
    public void compileFailed(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION FAILED");

            record(
                    EventType.COMPILATION_FAILURE,
                    null,
                    Map.of()
            );
        }
    }

    @Override
    public void compileError(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {

            log("COMPILATION ERROR");

            System.out.println(
                    "Line: " + event.getErrorLineNumber());

            System.out.println(
                    "Message: " + event.getErrorMessage());

            record(
                    EventType.COMPILATION_ERROR,
                    null,
                    Map.of(
                            "line",
                            String.valueOf(event.getErrorLineNumber()),
                            "message",
                            String.valueOf(event.getErrorMessage())
                    )
            );
        }
    }

    @Override
    public void compileWarning(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {

            log("COMPILATION WARNING");

            System.out.println(
                    "Message: " + event.getErrorMessage());

            record(
                    EventType.COMPILATION_WARNING,
                    null,
                    Map.of(
                            "line",
                            String.valueOf(event.getErrorLineNumber()),
                            "message",
                            String.valueOf(event.getErrorMessage())
                    )
            );
        }
    }

    
    @Override
    public void invocationFinished(InvocationFinishedEvent event) {

        if (!sessionManager.isActive()) {
            return;
        }

        String className = event.getClassName();
        String methodName = event.getMethodName();

        Map<String, String> details = new HashMap<>();

        details.put("method",
                methodName == null ? "<constructor>" : methodName);

        details.put("status",
                event.getEventType() == null
                        ? "UNKNOWN"
                        : event.getEventType().name());

        String objectName = event.getObjectName();

        if (objectName != null) {
            details.put("object_name", objectName);
        }

        String[] parameters = event.getParameters();

        if (parameters != null) {
            details.put("parameter_count",
                    String.valueOf(parameters.length));

            // Only store the parameter count, not potentially
            // sensitive argument values.
        }

        Object result = event.getResult();

        if (result != null) {
            details.put("result_type",
                    result.getClass().getSimpleName());
        }

        record(EventType.PROGRAM_EXECUTED, className, details);

        log("PROGRAM EXECUTED: "
                + className
                + " | "
                + details.get("method")
                + " | "
                + details.get("status"));

        if (event.getEventType()
                == InvocationFinishedEvent.EventType.EXCEPTION_EXIT) {

            Map<String, String> errorDetails =
                    new HashMap<>(details);

            errorDetails.put("error_category",
                    "RUNTIME_EXCEPTION");

            record(EventType.RUNTIME_ERROR,
                    className, errorDetails);

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

            record(
                    EventType.CLASS_CREATED,
                    name,
                    Map.of()
            );

            return;
        }

        if (!event.isClassCompiled() && !event.hasError()) {
            log("CLASS MODIFIED: " + name);

            record(
                    EventType.CLASS_MODIFIED,
                    name,
                    Map.of()
            );
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

        log("CLASS RENAMED: "
                + event.getOldName()
                + " -> "
                + event.getBClass().getName());

        record(
                EventType.CLASS_RENAMED,
                event.getBClass().getName(),
                Map.of(
                        "oldName", event.getOldName(),
                        "newName", event.getBClass().getName()
                )
        );
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

        record(
                EventType.CLASS_DELETED,
                name,
                Map.of()
        );
    }

    private static boolean isSourceFile(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(".java") || name.endsWith(".stride");
    }

    private Map<Path, Long> readSourceTimes(Path directory) {
        Map<Path, Long> result = new HashMap<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(BlueJTelemetryExtension::isSourceFile)
                    .forEach(file -> {
                        try {
                            result.put(file,
                                    Files.getLastModifiedTime(file).toMillis());
                        } catch (IOException e) {
                            log("Could not check source file: " + file);
                        }
                    });
        } catch (IOException e) {
            log("Could not scan source directory: " + e.getMessage());
        }
        return result;
    }

    private void registerSourceDirectory(Path directory) {
        sourceFileTimes.put(directory, readSourceTimes(directory));
    }

    private void checkSavedEdits() {
        if (!sessionManager.isActive()) {
            return;
        }
        try {
            for (Map.Entry<Path, Map<Path, Long>> directoryEntry
                    : sourceFileTimes.entrySet()) {
                Map<Path, Long> current = readSourceTimes(directoryEntry.getKey());
                Map<Path, Long> previous = directoryEntry.getValue();
                for (Map.Entry<Path, Long> fileEntry : current.entrySet()) {
                    Long oldTime = previous.get(fileEntry.getKey());
                    if (oldTime != null && fileEntry.getValue() > oldTime) {
                        record(EventType.EDIT_ACTIVITY,
                                fileEntry.getKey().getFileName().toString(),
                                Map.of("source", "saved-file-change",
                                        "file", fileEntry.getKey().getFileName().toString(),
                                        "interval_seconds", "5"));
                    }
                }
                sourceFileTimes.put(directoryEntry.getKey(), current);
            }
        } catch (RuntimeException e) {
            log("Edit monitor failed: " + e.getMessage());
        }
    }

    private void record(
            EventType type,
            String className,
            Map<String, String> details) {

        TelemetryEvent event = new TelemetryEvent(
                type,
                LocalDateTime.now(),
                currentProjectName,
                className,
                details
        );

        eventBus.publish(event);

        log("RECORDED: " + type);
    }

    private void log(String message) {
        System.out.println(
                LocalDateTime.now() + " | " + message);
    }

    @Override
    public boolean isCompatible() {
        return getExtensionsAPIVersionMajor() >= 3;
    }

    @Override
    public void terminate() {

        if (sessionManager.isActive()) {
            record(EventType.PROJECT_CLOSED, null, Map.of());
            sessionManager.endSession();

            reportService.exportReports(sessionManager);
        }

        editMonitor.shutdownNow();
        reportService.shutdown();
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
}
