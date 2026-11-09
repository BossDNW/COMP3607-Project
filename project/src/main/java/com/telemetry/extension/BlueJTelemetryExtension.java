
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

import java.time.LocalDateTime;

public class BlueJTelemetryExtension extends Extension implements PackageListener, CompileListener, ClassListener {
    
    private final Map<String, Set<String>> knownClasses = new ConcurrentHashMap<>();

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

        System.out.println("BlueJ Telemetry started!");
    }

    
    @Override
    public void packageOpened(PackageEvent event) {
        try {
            String name = event.getPackage().getProject().getName();
            String key = packageKey(event.getPackage());

            Set<String> classes = ConcurrentHashMap.newKeySet();

            for (BClass bClass : event.getPackage().getClasses()) {
                classes.add(bClass.getName());
            }

            knownClasses.put(key, classes);

            log("PROJECT OPENED: " + name);
        } catch (ExtensionException e) {
            log("Unable to identify project: " + e.getMessage());
        }
    }


    @Override
    public void packageClosing(PackageEvent event) {
        System.out.println("PACKAGE CLOSING");
    }

    @Override
    public void compileStarted(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION STARTED");
        }
    }

    @Override
    public void compileSucceeded(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION SUCCESS");
        }
    }

    @Override
    public void compileFailed(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION FAILED");
        }
    }

    @Override
    public void compileError(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION ERROR");
            System.out.println("Line: " + event.getErrorLineNumber());
            System.out.println("Message: " + event.getErrorMessage());
        }
    }

    @Override
    public void compileWarning(CompileEvent event) {
        if (event.isUserGeneratedCompilation()) {
            log("COMPILATION WARNING");
            System.out.println("Message: " + event.getErrorMessage());
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
            return;
        }

        if (!event.isClassCompiled() && !event.hasError()) {
            log("CLASS MODIFIED: " + name);
        }
    }


    @Override
    public void classNameChanged(ClassEvent event) {
        String key = packageKey(event.getPackage());
        Set<String> classes = knownClasses.get(key);

        if (key == null) {
            return;
        }

        if (classes != null) {
            classes.remove(event.getOldName());
            classes.add(event.getBClass().getName());
        }

        log("CLASS RENAMED: " + event.getOldName()
            + " -> " + event.getBClass().getName());
    }

    @Override
    public void classRemoved(ClassEvent event) {
        String name = event.getBClass().getName();
        String key = packageKey(event.getPackage());
        Set<String> classes = knownClasses.get(key);

        if (key == null) {
            return;
        }

        if (classes != null) {
            classes.remove(name);
        }

        log("CLASS DELETED: " + name);
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
}
