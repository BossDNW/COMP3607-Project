package com.telemetry.reports;

import com.telemetry.events.EventType;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.session.SessionManager;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PdfReportGeneratorTest {

    @TempDir
    Path tempDir;

    private TelemetryEvent event(
            EventType type,
            String className,
            Map<String, String> details) {

        return new TelemetryEvent(
                type,
                LocalDateTime.now(),
                "TestProject",
                className,
                details
        );
    }

    private SessionManager createSession() {
        SessionManager session = new SessionManager();
        session.startSession();

        session.recordEvent(event(
                EventType.PROJECT_OPENED, null, Map.of()));

        session.recordEvent(event(
                EventType.CLASS_CREATED, "HelloWorld", Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_STARTED, null, Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_SUCCESS, null, Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_STARTED, null, Map.of()));

        session.recordEvent(event(
                EventType.COMPILATION_ERROR, null,
                Map.of("line", "10",
                       "message", "';' expected")));

        session.recordEvent(event(
                EventType.COMPILATION_FAILURE, null, Map.of()));

        session.endSession();
        return session;
    }

    @Test
    void testPdfFileCreatedWithTwoPages() throws Exception {
        SessionManager session = createSession();
        Path output = tempDir.resolve("report.pdf");

        new PdfReportGenerator().generate(session, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);

        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {
            assertEquals(2, pdf.getNumberOfPages());
        }
    }

    @Test
    void testPdfContainsCorrectStatistics() throws Exception {
        SessionManager session = createSession();
        Path output = tempDir.resolve("statistics.pdf");

        new PdfReportGenerator().generate(session, output);

        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {

            String contents = new PDFTextStripper().getText(pdf);

            assertTrue(contents.contains(
                    "BlueJ Telemetry - Session Report"));

            assertTrue(contents.contains(
                    "Total compilations: 2"));

            assertTrue(contents.contains(
                    "Successful compilations: 1"));

            assertTrue(contents.contains(
                    "Failed compilations: 1"));

            assertTrue(contents.contains(
                    "Compilation success rate: 50.0%"));

            assertTrue(contents.contains(
                    "Estimated active time:"));

            assertTrue(contents.contains(
                    "Estimated idle time:"));

            assertTrue(contents.contains(
                    "CLASS_CREATED"));

            assertTrue(contents.contains(
                    "HelloWorld"));
        }
    }

    @Test
    void testPdfHandlesEmptySession() throws Exception {
        SessionManager session = new SessionManager();
        session.startSession();
        session.endSession();

        Path output = tempDir.resolve("empty.pdf");

        new PdfReportGenerator().generate(session, output);

        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {

            assertEquals(2, pdf.getNumberOfPages());

            String contents = new PDFTextStripper().getText(pdf);

            assertTrue(contents.contains(
                    "Total compilations: 0"));

            assertTrue(contents.contains(
                    "No class activity detected."));

            assertTrue(contents.contains(
                    "No compiler errors detected."));
        }
    }
}
