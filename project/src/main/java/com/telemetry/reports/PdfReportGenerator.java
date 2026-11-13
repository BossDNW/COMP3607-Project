
package com.telemetry.reports;

import com.telemetry.analytics.ActivityTimeAnalyzer;
import com.telemetry.analytics.TelemetryAnalyzer;
import com.telemetry.events.EventType;
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

    private static final PDType1Font NORMAL =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private static final PDType1Font BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final Color NAVY = new Color(32, 48, 78);
    private static final Color BLUE = new Color(65, 125, 205);
    private static final Color RED = new Color(200, 70, 75);
    private static final Color GREEN = new Color(43, 150, 112);
    private static final Color GRAY = new Color(190, 195, 205);

    @Override
    public void export(SessionManager session, Path output)
            throws IOException {
        generate(session, output);
    }

    public void generate(SessionManager session, Path output)
            throws IOException {

        List<TelemetryEvent> events = session.getEvents();

        TelemetryAnalyzer analyzer =
                new TelemetryAnalyzer(events);

        ActivityTimeAnalyzer activity =
                new ActivityTimeAnalyzer(events);

        long active = activity.getActiveTimeSeconds(
                session.getSessionStart(),
                session.getSessionEnd());

        long idle = activity.getIdleTimeSeconds(
                session.getSessionStart(),
                session.getSessionEnd());

        Path parent = output.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PDDocument document = new PDDocument()) {

            // ============================
            // PAGE 1 - SESSION SUMMARY
            // ============================

            PDPage page1 = new PDPage(PDRectangle.A4);
            document.addPage(page1);

            try (PDPageContentStream cs =
                    new PDPageContentStream(document, page1)) {

                heading(cs,
                        "BlueJ Telemetry - Session Report",
                        780);

                text(cs,
                        "Student ID: " + JsonExporter.studentId(),
                        50, 755);

                text(cs,
                        "Session start: "
                                + session.getSessionStart(),
                        50, 735);

                text(cs,
                        "Session end: "
                                + session.getSessionEnd(),
                        50, 720);

                section(cs, "Session Summary", 690);

                text(cs,
                        "Duration: "
                                + session.getSessionDuration()
                                        .toSeconds()
                                + " seconds",
                        50, 670);

                text(cs,
                        "Total compilations: "
                                + analyzer.getTotalCompilations(),
                        50, 652);

                text(cs,
                        "Successful compilations: "
                                + analyzer.getSuccessfulCompilations(),
                        50, 634);

                text(cs,
                        "Failed compilations: "
                                + analyzer.getFailedCompilations(),
                        50, 616);

                text(cs,
                        String.format(
                                "Compilation success rate: %.1f%%",
                                analyzer.getCompilationSuccessRate()),
                        50, 598);

                // CHART 1
                section(cs,
                        "Chart 1 - Compilation Outcomes",
                        572);

                stackedBar(cs,
                        50, 540,
                        450, 20,
                        analyzer.getSuccessfulCompilations(),
                        analyzer.getFailedCompilations(),
                        BLUE, RED);

                text(cs,
                        "Successful: "
                                + analyzer.getSuccessfulCompilations(),
                        50, 522);

                text(cs,
                        "Failed: "
                                + analyzer.getFailedCompilations(),
                        265, 522);

                // ERROR SUMMARY
                section(cs, "Error Summary", 490);

                text(cs,
                        "Compiler errors: "
                                + analyzer.getTotalErrors(),
                        50, 471);

                text(cs,
                        "Warnings: "
                                + count(
                                        events,
                                        EventType.COMPILATION_WARNING),
                        50, 455);

                if (analyzer.getTotalErrors() == 0) {

                    text(cs,
                            "No compiler errors detected.",
                            50, 438);

                } else {

                    int errorY = 438;

                    for (Map.Entry<String, Integer> entry
                            : analyzer.getErrorFrequencies()
                                    .entrySet()) {

                        if (errorY < 413) {
                            break;
                        }

                        text(cs,
                                entry.getKey()
                                        + " ("
                                        + entry.getValue()
                                        + ")",
                                60, errorY);

                        errorY -= 15;
                    }
                }

                // CHART 2
                section(cs,
                        "Chart 2 - Estimated Active vs Idle Time",
                        390);

                stackedBar(cs,
                        50, 358,
                        450, 20,
                        active, idle,
                        GREEN, GRAY);

                text(cs,
                        "Estimated active time: "
                                + active
                                + " seconds",
                        50, 339);

                text(cs,
                        "Estimated idle time: "
                                + idle
                                + " seconds",
                        280, 339);

                text(cs,
                        "Estimation threshold: 180 seconds",
                        50, 321);

                // DEVELOPMENT SUMMARY
                section(cs, "Development Summary", 287);

                text(cs,
                        "Classes created: "
                                + count(
                                        events,
                                        EventType.CLASS_CREATED),
                        50, 265);

                text(cs,
                        "Classes modified: "
                                + count(
                                        events,
                                        EventType.CLASS_MODIFIED),
                        250, 265);

                text(cs,
                        "Classes renamed: "
                                + count(
                                        events,
                                        EventType.CLASS_RENAMED),
                        50, 245);

                text(cs,
                        "Classes deleted: "
                                + count(
                                        events,
                                        EventType.CLASS_DELETED),
                        250, 245);

                text(cs,
                        "Saved edit events: "
                                + count(
                                        events,
                                        EventType.EDIT_ACTIVITY),
                        50, 225);

                text(cs,
                        "Program executions: "
                                + count(
                                        events,
                                        EventType.PROGRAM_EXECUTED),
                        50, 205);

                text(cs,
                        "Runtime exceptions: "
                                + count(
                                        events,
                                        EventType.RUNTIME_ERROR),
                        50, 185);

                footer(cs, "Page 1 of 2");
            }

            // ============================
            // PAGE 2 - DEVELOPMENT DETAILS
            // ============================

            PDPage page2 = new PDPage(PDRectangle.A4);
            document.addPage(page2);

            try (PDPageContentStream cs =
                    new PDPageContentStream(document, page2)) {

                heading(cs,
                        "BlueJ Telemetry - Development Timeline",
                        780);

                section(cs,
                        "Class, Editing and Execution Events",
                        750);

                int y = 727;
                int displayed = 0;
                long totalTimeline = 0;

                for (TelemetryEvent event : events) {

                    if (!isTimelineEvent(event.getType())) {
                        continue;
                    }

                    totalTimeline++;

                    if (displayed >= 11) {
                        continue;
                    }

                    String line =
                            event.getTimestamp()
                                    .toLocalTime()
                                    .withNano(0)
                            + " | "
                            + event.getType()
                            + " | "
                            + shortName(event);

                    text(cs, line, 50, y);

                    y -= 19;
                    displayed++;
                }

                if (displayed == 0) {

                    text(cs,
                            "No class activity detected.",
                            50, 727);

                } else if (totalTimeline > displayed) {

                    text(cs,
                            "Additional events in JSON: "
                                    + (totalTimeline - displayed),
                            50, 503);
                }

                // COMPILATION INTERVALS
                section(cs,
                        "Compilation Intervals",
                        475);

                text(cs,
                        String.format(
                                "Average interval: %.2f seconds",
                                analyzer.getAverageCompilationInterval()),
                        50, 455);

                text(cs,
                        "Intervals (seconds): "
                                + analyzer.getCompilationIntervals(),
                        50, 438);

                // CHART 3
                section(cs,
                        "Chart 3 - Development Activity",
                        410);

                String[] labels = {
                        "Saved edits",
                        "Class changes",
                        "Executions",
                        "Runtime errors"
                };

                long[] values = {
                        count(events, EventType.EDIT_ACTIVITY),

                        count(events, EventType.CLASS_CREATED)
                                + count(
                                        events,
                                        EventType.CLASS_MODIFIED)
                                + count(
                                        events,
                                        EventType.CLASS_RENAMED)
                                + count(
                                        events,
                                        EventType.CLASS_DELETED),

                        count(events, EventType.PROGRAM_EXECUTED),

                        count(events, EventType.RUNTIME_ERROR)
                };

                horizontalBars(cs, labels, values, 385);

                // RUNTIME AND COMPILATION ERRORS
                section(cs,
                        "Runtime and Error Details",
                        235);

                int detailsY = 215;
                int errorsShown = 0;

                for (TelemetryEvent event : events) {

                    if (event.getType() != EventType.RUNTIME_ERROR
                            && event.getType()
                                    != EventType.COMPILATION_ERROR) {
                        continue;
                    }

                    if (detailsY < 115) {
                        break;
                    }

                    String message =
                            event.getDetails()
                                    .getOrDefault(
                                            "message",
                                            event.getDetails()
                                                    .getOrDefault(
                                                            "error_category",
                                                            event.getType()
                                                                    .name()));

                    text(cs,
                            event.getTimestamp()
                                    .toLocalTime()
                                    .withNano(0)
                                    + " | "
                                    + message,
                            50, detailsY);

                    detailsY -= 18;
                    errorsShown++;
                }

                if (errorsShown == 0) {

                    text(cs,
                            "No compilation or runtime errors recorded.",
                            50, 215);
                }

                footer(cs, "Page 2 of 2");
            }

            document.save(output.toFile());
        }
    }

    // ============================
    // EVENT HELPERS
    // ============================

    private boolean isTimelineEvent(EventType type) {

        return type == EventType.CLASS_CREATED
                || type == EventType.CLASS_MODIFIED
                || type == EventType.CLASS_RENAMED
                || type == EventType.CLASS_DELETED
                || type == EventType.EDIT_ACTIVITY
                || type == EventType.PROGRAM_EXECUTED
                || type == EventType.RUNTIME_ERROR;
    }

    private String shortName(TelemetryEvent event) {

        String name = event.getClassName();
        String method = event.getDetails().get("method");

        if (name == null) {
            name = "";
        }

        if (method != null && !method.isBlank()) {
            name += "." + method;
        }

        return name;
    }

    private long count(
            List<TelemetryEvent> events,
            EventType type) {

        return events.stream()
                .filter(event -> event.getType() == type)
                .count();
    }

    // ============================
    // CHART HELPERS
    // ============================

    private void stackedBar(
            PDPageContentStream cs,
            float x,
            float y,
            float width,
            float height,
            long first,
            long second,
            Color firstColor,
            Color secondColor)
            throws IOException {

        long total = first + second;

        if (total == 0) {

            cs.setNonStrokingColor(GRAY);
            cs.addRect(x, y, width, height);
            cs.fill();

            return;
        }

        float firstWidth =
                width * (float) first / total;

        float secondWidth =
                width - firstWidth;

        if (firstWidth > 0) {

            cs.setNonStrokingColor(firstColor);
            cs.addRect(x, y, firstWidth, height);
            cs.fill();
        }

        if (secondWidth > 0) {

            cs.setNonStrokingColor(secondColor);

            cs.addRect(
                    x + firstWidth,
                    y,
                    secondWidth,
                    height);

            cs.fill();
        }
    }

    private void horizontalBars(
            PDPageContentStream cs,
            String[] labels,
            long[] values,
            int startY)
            throws IOException {

        long max = 1;

        for (long value : values) {
            max = Math.max(max, value);
        }

        for (int i = 0; i < labels.length; i++) {

            int y = startY - i * 32;

            text(cs, labels[i], 50, y);

            float barWidth =
                    270f * values[i] / max;

            if (barWidth > 0) {

                cs.setNonStrokingColor(BLUE);
                cs.addRect(
                        170,
                        y - 2,
                        barWidth,
                        13);
                cs.fill();
            }

            text(cs,
                    String.valueOf(values[i]),
                    455,
                    y);
        }
    }

    // ============================
    // PDF TEXT HELPERS
    // ============================

    private void heading(
            PDPageContentStream cs,
            String value,
            int y)
            throws IOException {

        cs.setNonStrokingColor(NAVY);
        write(cs, value, 50, y, 18, BOLD);
    }

    private void section(
            PDPageContentStream cs,
            String value,
            int y)
            throws IOException {

        cs.setNonStrokingColor(NAVY);
        write(cs, value, 50, y, 13, BOLD);
    }

    private void text(
            PDPageContentStream cs,
            String value,
            int x,
            int y)
            throws IOException {

        cs.setNonStrokingColor(Color.DARK_GRAY);
        write(cs, value, x, y, 10, NORMAL);
    }

    private void footer(
            PDPageContentStream cs,
            String value)
            throws IOException {

        text(cs, value, 500, 35);
    }

    private void write(
            PDPageContentStream cs,
            String value,
            int x,
            int y,
            int size,
            PDType1Font font)
            throws IOException {

        String safe = value == null
                ? ""
                : value.replaceAll("[^\\x20-\\x7E]", "?");

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
