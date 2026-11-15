package com.telemetry.reporting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import com.telemetry.integration.Session;
import com.telemetry.integration.TelemetryEvent;
import com.telemetry.monitoring.SessionStats;

public class PdfReport implements ReportExporter
{
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    @Override 
    public void export(Session session, Path output) throws IOException
    {
        Path parent = output.toAbsolutePath().getParent();
        if(parent != null) Files.createDirectories(parent);

        SessionStats stats = session.getStats();

        try(PDDocument pdf = new PDDocument())
        {
            PDPage first = new PDPage(PDRectangle.A4);
            pdf.addPage(first);

            try(PDPageContentStream page = new PDPageContentStream(pdf, first))
            {
                line(page, "BlueJ Telemetry Session Report", 50, 780, true);

                line(page, "Student ID: " + session.getStudentId(), 50, 750, false);
                line(page, "Project: " + session.getProject(), 50, 732, false);

                line(page, "Session duration: " + session.durationSeconds() + " seconds", 50, 714, false);
                line(page, "Total compilations: " + stats.getAttempts(), 50, 682, false);
                line(page, "Successful compilations: " + stats.getSuccesses(), 50, 664, false);
                line(page, "Failed compilations: " + stats.getFailures(), 50, 646, false);
                line(page, String.format("Compilation success rate: %.1f%%", stats.getSuccessRate()), 50, 628, false);

                line(page, "Chart 1: Compilation Results", 50, 592, true);
                drawBar(page, 50, 555, 450, stats.getSuccesses(), stats.getFailures());
                line(page, "Success: " + stats.getSuccesses() + " Failed: " + stats.getFailures(), 50, 535, false);

                long active = SessionStats.activeSeconds(session);
                long idle = SessionStats.idleSeconds(session);
                line(page, "Chart 2: Active and Idle Time (estimated)", 50, 483, true);
                drawBar(page, 50, 446, 450, active, idle);
                line(page, "Estimated active time: " + active + " seconds", 50, 427, false);
                line(page, "Estimated idle time: " + idle + " seconds", 270, 427, false);
                line(page, "Idle threshold: 180 seconds", 50, 409, false);

                line(page, "Coding Activity", 50, 350, true);
                line(page, "Saved edit events: " + stats.getEdits(), 50, 326, false);
                line(page, "Program executions: " + stats.getRuns(), 50, 308, false);
                line(page, "Runtime exceptions: " + stats.getRuntimeErrors(), 50, 290, false);

                line(page, "Page 1 of 2", 480, 42, false);
            }

            PDPage second = new PDPage(PDRectangle.A4);
            pdf.addPage(second);

            try(PDPageContentStream page = new PDPageContentStream(pdf, second))
            {
                line(page, "BlueJ Telemetry - Errors and Timeline", 50, 780, true);

                line(page, "Compilation Error Frequencies", 50, 744, true);

                int y = 722;
                Map<String, Integer> errors =
                        stats.getErrors();

                if (errors.isEmpty())
                {
                    line(page,
                            "No compiler errors detected.",
                            50, y, false);
                }
                else 
                {
                    for (Map.Entry<String, Integer> entry : errors.entrySet())
                    {
                        if (y < 625) break;
                        line(page, entry.getValue() + " x " + entry.getKey(), 50, y, false);
                        y -= 18;
                    }
                }

                line(page, "Compilation intervals (seconds): " + stats.getCompileIntervals(), 50, 600, false);

                line(page, "Development Timeline", 50, 558, true);

                int timelineY = 535;
                int shown = 0;

                List<TelemetryEvent> events = session.getEvents();

                for (TelemetryEvent event : events)
                {
                    if (!event.getType().startsWith("CLASS_")
                            && !event.getType().equals("EDIT_ACTIVITY")
                            && !event.getType().equals("PROGRAM_EXECUTED")
                            && !event.getType().equals("RUNTIME_ERROR")) continue;

                    if (timelineY < 90) break;

                    String time = event.getTime().toLocalTime().withNano(0).toString();

                    line(page, time + "  " + event.getType() + "  " + event.getName(), 50, timelineY, false);

                    timelineY -= 19;
                    shown++;
                }

                if (shown == 0) line(page, "No class activity detected.", 50, 535, false);

                if (events.size() > shown) line(page, "Full event history is available in JSON.", 50, 75, false);

                line(page, "Page 2 of 2", 480, 42, false);
            }

            pdf.save(output.toFile());
        }
    }

    private void drawBar(PDPageContentStream page, float x, float y, float width, long first, long second) throws IOException
    {
        long total = first + second;
        float filled = total == 0 ? 0 : width / total;

        page.setNonStrokingColor(70, 120, 185);

        if(filled > 0)
        {
            page.addRect(x, y, filled, 18);
            page.fill();
        }

        page.setNonStrokingColor(200, 205, 215);

        if(width - filled > 0)
        {
            page.addRect(x + filled, y, width - filled, 18);
            page.fill();
        }
    }

    private void line(PDPageContentStream page, String value, float x, float y, boolean heading) throws IOException
    {
        String safe = value == null ? "" : value.replaceAll("[^\\x20-\\x7E]", "?");

        if (safe.length() > 88) safe = safe.substring(0, 85) + "...";

        page.setNonStrokingColor(35, 45, 60);
        page.beginText();
        page.setFont(heading ? FONT_BOLD : FONT, heading ? 14 : 10);
        page.newLineAtOffset(x, y);
        page.showText(safe);
        page.endText();
    }
}
