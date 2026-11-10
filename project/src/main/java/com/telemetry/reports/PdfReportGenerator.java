package com.telemetry.reports;

import com.telemetry.analytics.TelemetryAnalyzer;
import com.telemetry.events.EventType;
import com.telemetry.analytics.ActivityTimeAnalyzer;
import com.telemetry.events.TelemetryEvent;
import com.telemetry.session.SessionManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class PdfReportGenerator implements ReportExporter {

        private static final PDType1Font NORMAL = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA);

        private static final PDType1Font BOLD = new PDType1Font(
                Standard14Fonts.FontName.HELVETICA_BOLD);

        private static final Color NAVY = new Color(32, 48, 78);
        private static final Color BLUE = new Color(65, 125, 205);
        private static final Color RED = new Color(200, 70, 75);
        

        @Override
        public void export(SessionManager session, Path output)
                throws IOException {

        generate(session, output);
        }

        public void generate(SessionManager session, Path output)
                throws IOException {

                List<TelemetryEvent> events = session.getEvents();
                TelemetryAnalyzer analyzer = new TelemetryAnalyzer(events);
                ActivityTimeAnalyzer activityAnalyzer = new ActivityTimeAnalyzer(events);

                Path parent = output.toAbsolutePath().getParent();

                if (parent != null) {
                        Files.createDirectories(parent);
                }

                try (PDDocument document = new PDDocument()) {

                        PDPage page1 = new PDPage(PDRectangle.A4);
                        document.addPage(page1);

                        try (PDPageContentStream cs =
                                        new PDPageContentStream(document, page1)) {

                        heading(cs, "BlueJ Telemetry - Session Report", 770);
                        text(cs, "Student ID: STUDENT_ID", 50, 735);
                        text(cs, "Session start: " + session.getSessionStart(), 50, 715);
                        text(cs, "Session end: " + session.getSessionEnd(), 50, 695);

                        section(cs, "Session Summary", 660);

                        text(cs, "Duration: " +
                                session.getSessionDuration().toSeconds() +
                                " seconds", 50, 635);

                        text(cs, "Total compilations: " +
                                analyzer.getTotalCompilations(), 50, 615);

                        text(cs, "Successful compilations: " +
                                analyzer.getSuccessfulCompilations(), 50, 595);

                        text(cs, "Failed compilations: " +
                                analyzer.getFailedCompilations(), 50, 575);

                        text(cs, String.format(
                                "Compilation success rate: %.1f%%",
                                analyzer.getCompilationSuccessRate()
                        ), 50, 555);

                        section(cs, "Compilation Outcomes", 510);

                        long success = analyzer.getSuccessfulCompilations();
                        long failed = analyzer.getFailedCompilations();
                        long total = success + failed;

                        float successWidth = total == 0 ? 0 :
                                400f * success / total;

                        float failedWidth = total == 0 ? 0 :
                                400f * failed / total;

                        cs.setNonStrokingColor(BLUE);
                        cs.addRect(50, 465, successWidth, 24);
                        cs.fill();

                        cs.setNonStrokingColor(RED);
                        cs.addRect(50 + successWidth, 465, failedWidth, 24);
                        cs.fill();

                        text(cs, "Successful: " + success, 50, 440);
                        text(cs, "Failed: " + failed, 210, 440);

                        section(cs, "Error Summary", 395);
                        text(cs, "Total compiler errors: " +
                                analyzer.getTotalErrors(), 50, 370);

                        int y = 345;

                        for (Map.Entry<String, Integer> entry :
                                analyzer.getErrorFrequencies().entrySet()) {

                                if (y < 280) {
                                break;
                                }

                                text(cs, entry.getKey() + " (" +
                                        entry.getValue() + ")", 60, y);

                                y -= 20;
                        }

                        section(cs, "Activity Time", 235);
                        
                        text(cs, "Estimated active time: " +
                                activityAnalyzer.getActiveTimeSeconds(
                                        session.getSessionStart(),
                                        session.getSessionEnd()
                                ) + " seconds", 50, 210);

                        text(cs, "Estimated idle time: " +
                                activityAnalyzer.getIdleTimeSeconds(
                                        session.getSessionStart(),
                                        session.getSessionEnd()
                                ) + " seconds", 50, 190);


                        footer(cs, "Page 1 of 2");
                        }

                        PDPage page2 = new PDPage(PDRectangle.A4);
                        document.addPage(page2);

                        try (PDPageContentStream cs =
                                        new PDPageContentStream(document, page2)) {

                        heading(cs, "BlueJ Telemetry - Development Timeline", 770);

                        section(cs, "Class Activity", 730);

                        int y = 700;
                        int displayed = 0;

                        for (TelemetryEvent event : events) {

                                if (event.getClassName() == null) {
                                continue;
                                }

                                if (displayed >= 12) {
                                break;
                                }

                                String line = event.getTimestamp()
                                        .toLocalTime().withNano(0)
                                        + " | " + event.getType()
                                        + " | " + event.getClassName();

                                text(cs, line, 50, y);
                                y -= 22;
                                displayed++;
                        }

                        if (displayed == 0) {
                                text(cs, "No class activity detected.", 50, y);
                        }

                        section(cs, "Compilation Intervals", 380);

                        text(cs, String.format(
                                "Average interval: %.2f seconds",
                                analyzer.getAverageCompilationInterval()
                        ), 50, 355);

                        text(cs, "Intervals (seconds): " +
                                analyzer.getCompilationIntervals(),
                                50, 330);

                        section(cs, "Error Frequencies", 285);

                        int errorY = 255;

                        for (Map.Entry<String, Integer> entry :
                                analyzer.getErrorFrequencies().entrySet()) {

                                if (errorY < 130) {
                                break;
                                }

                                text(cs, entry.getKey() + ": " +
                                        entry.getValue(), 50, errorY);

                                errorY -= 22;
                        }

                        if (analyzer.getErrorFrequencies().isEmpty()) {
                                text(cs, "No compiler errors detected.", 50, 255);
                        }

                        footer(cs, "Page 2 of 2");
                        }

                        document.save(output.toFile());
                }
        }

        private void heading(PDPageContentStream cs,
                                String value, int y) throws IOException {

                cs.setNonStrokingColor(NAVY);
                write(cs, value, 50, y, 18, BOLD);
        }

        private void section(PDPageContentStream cs,
                                String value, int y) throws IOException {

                cs.setNonStrokingColor(NAVY);
                write(cs, value, 50, y, 13, BOLD);
        }

        private void text(PDPageContentStream cs,
                        String value, int x, int y)
                throws IOException {

                cs.setNonStrokingColor(Color.DARK_GRAY);
                write(cs, value, x, y, 10, NORMAL);
        }

        private void footer(PDPageContentStream cs,
                        String value) throws IOException {

                text(cs, value, 500, 35);
        }

        private void write(PDPageContentStream cs, String value,
                        int x, int y, int size, PDType1Font font)
                throws IOException {

                // Standard PDF fonts support a limited character set.
                String safe = value == null ? "" :
                        value.replaceAll("[^\\x20-\\x7E]", "?");

                if (safe.length() > 90) {
                        safe = safe.substring(0, 87) + "...";
                }

                cs.beginText();
                cs.setFont(font, size);
                cs.newLineAtOffset(x, y);
                cs.showText(safe);
                cs.endText();
        }
}
