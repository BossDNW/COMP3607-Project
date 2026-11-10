package com.telemetry.reports;

import com.telemetry.session.SessionManager;

import java.io.IOException;
import java.nio.file.Path;

public abstract class ReportExporterCreator {

    // Factory Method
    public abstract ReportExporter createExporter();

    // Shared export workflow
    public void exportReport(SessionManager session, Path output)
            throws IOException {

        ReportExporter exporter = createExporter();
        exporter.export(session, output);
    }
}
