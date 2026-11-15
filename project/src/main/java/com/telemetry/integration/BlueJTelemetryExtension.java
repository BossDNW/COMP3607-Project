
package com.telemetry.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import bluej.extensions2.*;
import bluej.extensions2.event.*;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;



public class BlueJTelemetryExtension extends Extension implements PackageListener, ClassListener, CompileListener, InvocationFinishedListener {

    private final Preferences settings =
            Preferences.userNodeForPackage(
                    BlueJTelemetryExtension.class);

    private final Map<String, Set<String>> knownClasses =
            new HashMap<>();

    private final Set<String> openPackages = new HashSet<>();

    private volatile Session session;

    @Override
    public void startup(BlueJ bluej) {

        bluej.addPackageListener(this);
        bluej.addClassListener(this);
        bluej.addCompileListener(this);
        bluej.addInvocationFinishedListener(this);

        bluej.setMenuGenerator(new MenuGenerator() {

            @Override
            public MenuItem getToolsMenuItem(BPackage pkg) {

                Menu menu = new Menu("BlueJ Telemetry");

                MenuItem export =
                        new MenuItem("Export Reports");

                // Temporarily disabled until reporting is ready.
                export.setDisable(true);

                MenuItem setId =
                        new MenuItem("Set Student ID");

                setId.setOnAction(e -> {

                    TextInputDialog dialog =
                            new TextInputDialog(
                                    settings.get("studentId", ""));

                    dialog.setTitle("BlueJ Telemetry");
                    dialog.setHeaderText("Student Identification");
                    dialog.setContentText("Student ID:");

                    dialog.showAndWait().ifPresent(id -> {

                        if (!id.isBlank()) {

                            settings.put("studentId", id.trim());

                            if (session != null) {
                                session.setStudentId(id.trim());
                            }
                        }
                    });
                });

                menu.getItems().addAll(export, setId);

                return menu;
            }
        });
    }

    @Override
    public void packageOpened(PackageEvent event) {

        try {
            BPackage pkg = event.getPackage();

            String key = pkg.getDir().getAbsolutePath();

            String projectName = pkg.getProject().getName();

            openPackages.add(key);

            Set<String> names = new HashSet<>();

            for (BClass cls : pkg.getClasses()) {
                names.add(cls.getName());
            }

            knownClasses.put(key, names);

            if (session == null || !session.isActive()) {

                session = new Session();

                session.setStudentId(
                        settings.get("studentId", "STUDENT_ID"));

                session.setProjectDirectory(
                        pkg.getProject().getDir().toPath());

                session.start(projectName);
            }

        } catch (ExtensionException ex) {

            System.err.println(
                    "BlueJ telemetry: " + ex.getMessage());
        }
    }

    @Override
    public void packageClosing(PackageEvent event) {

        String key = directory(event.getPackage());

        if (key != null) {
            openPackages.remove(key);
            knownClasses.remove(key);
        }

        if (openPackages.isEmpty()) {
            finishSession();
        }
    }

    private String directory(BPackage pkg) {

        try {
            return pkg.getDir().getAbsolutePath();

        } catch (ExtensionException ex) {
            return null;
        }
    }

    private void record(
            String type,
            String name,
            String details) {

        Session current = session;

        if (current != null) {
            current.record(type, name, details);
        }
    }

    @Override
    public void classStateChanged(ClassEvent event) {

        String key = directory(event.getPackage());

        Set<String> names = knownClasses.get(key);

        if (names == null) {
            return;
        }

        String name = event.getBClass().getName();

        if (names.add(name)) {

            record("CLASS_CREATED", name, "");

        } else if (!event.isClassCompiled()
                && !event.hasError()) {

            record("CLASS_MODIFIED", name, "");
        }
    }

    @Override
    public void classNameChanged(ClassEvent event) {

        String key = directory(event.getPackage());

        Set<String> names = knownClasses.get(key);

        String oldName = event.getOldName();

        String newName =
                event.getBClass().getName();

        if (names != null) {
            names.remove(oldName);
            names.add(newName);
        }

        record(
                "CLASS_RENAMED",
                newName,
                "Previously: " + oldName);
    }

    @Override
    public void classRemoved(ClassEvent event) {

        String key = directory(event.getPackage());

        String name =
                event.getBClass().getName();

        Set<String> names = knownClasses.get(key);

        if (names != null) {
            names.remove(name);
        }

        record("CLASS_DELETED", name, "");
    }

    @Override
    public void compileStarted(CompileEvent event) {

        if (event.isUserGeneratedCompilation()) {
            record("COMPILATION_STARTED", "", "");
        }
    }

    @Override
    public void compileSucceeded(CompileEvent event) {

        if (event.isUserGeneratedCompilation()) {
            record("COMPILATION_SUCCESS", "", "");
        }
    }

    @Override
    public void compileFailed(CompileEvent event) {

        if (event.isUserGeneratedCompilation()) {
            record("COMPILATION_FAILURE", "", "");
        }
    }

    @Override
    public void compileError(CompileEvent event) {

        if (event.isUserGeneratedCompilation()) {

            record(
                    "COMPILATION_ERROR",
                    "",
                    "Line " + event.getErrorLineNumber()
                            + ": " + event.getErrorMessage());
        }
    }

    @Override
    public void compileWarning(CompileEvent event) {

        if (event.isUserGeneratedCompilation()) {

            record(
                    "COMPILATION_WARNING",
                    "",
                    event.getErrorMessage());
        }
    }

    @Override
    public void invocationFinished(
            InvocationFinishedEvent event) {

        if (session == null || !session.isActive()) {
            return;
        }

        String name = event.getClassName();

        String method =
                event.getMethodName() == null
                        ? "<constructor>"
                        : event.getMethodName();

        String status =
                event.getEventType() == null
                        ? "UNKNOWN"
                        : event.getEventType().name();

        record(
                "PROGRAM_EXECUTED",
                name,
                method + ": " + status);

        if (event.getEventType()
                == InvocationFinishedEvent.EventType.EXCEPTION_EXIT) {

            record(
                    "RUNTIME_ERROR",
                    name,
                    method + ": " + status);
        }
    }

    private void finishSession() {

        Session current = session;

        if (current != null && current.isActive()) {
            current.finish();
        }
    }

    @Override
    public void terminate() {
        finishSession();
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
        return "2.0";
    }

    @Override
    public String getDescription() {
        return "BlueJ coding session tracker";
    }
}
