package com.telemetry.reports;

import com.telemetry.session.SessionManager;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReportService {

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "telemetry-report-worker");
                thread.setDaemon(true);
                return thread;
            });

    private final ReportExporterCreator jsonCreator =
            new JsonExporterCreator();

    private final ReportExporterCreator pdfCreator =
            new PdfExporterCreator();

    private final Path reportDirectory = Path.of(
            System.getProperty("user.home"),
            "bluej",
            "telemetry-reports"
    );

    public void startAutomaticBackups(SessionManager session) {

        executor.scheduleAtFixedRate(() -> {

            if (!session.isActive()) {
                return;
            }

            SessionManager snapshot = session.snapshot();

            try {
                Path output = reportDirectory.resolve(
                        "autosave-current.json"
                );

                jsonCreator.exportReport(snapshot, output);

                System.out.println(
                        "TELEMETRY AUTOSAVE: " + output
                );

            } catch (Exception e) {
                System.err.println(
                        "TELEMETRY AUTOSAVE FAILED: "
                                + e.getMessage()
                );
            }

        }, 60, 60, TimeUnit.SECONDS);
    }

    public void exportReports(SessionManager session) {

        // Capture the session BEFORE scheduling the export.
        SessionManager snapshot = session.snapshot();

        executor.execute(() -> {
            try {
                String timestamp = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyyMMdd-HHmmss-SSS"
                        )
                );

                String filename = "session-" + timestamp;

                Path jsonOutput = reportDirectory.resolve(
                        filename + ".json"
                );

                Path pdfOutput = reportDirectory.resolve(
                        filename + ".pdf"
                );

                jsonCreator.exportReport(snapshot, jsonOutput);
                System.out.println(
                        "JSON EXPORTED: " + jsonOutput
                );

                pdfCreator.exportReport(snapshot, pdfOutput);
                System.out.println(
                        "PDF EXPORTED: " + pdfOutput
                );

            } catch (IOException e) {
                System.err.println(
                        "REPORT EXPORT FAILED: " + e.getMessage()
                );
            }
        });
    }

    public void shutdown() {
        executor.shutdown();

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println(
                        "Report worker still finishing exports."
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
