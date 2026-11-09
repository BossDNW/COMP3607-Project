
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

import java.time.LocalDateTime;

public class BlueJTelemetryExtension extends Extension
        implements PackageListener, CompileListener, ClassListener {

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
            String name = event.getPackage()
                               .getProject()
                               .getName();

            System.out.println("PROJECT OPENED: " + name);
        } catch (ExtensionException e) {
            System.out.println("Unable to identify project.");
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
        String className = event.getBClass().getName();

        if (!event.isClassCompiled() && !event.hasError()) {
            log("CLASS MODIFIED: " + className);
        }
    }

    @Override
    public void classNameChanged(ClassEvent event) {
        log("CLASS RENAMED: " + event.getOldName()
            + " -> " + event.getBClass().getName());
    }

    @Override
    public void classRemoved(ClassEvent event) {
        log("CLASS DELETED: " + event.getBClass().getName());
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
