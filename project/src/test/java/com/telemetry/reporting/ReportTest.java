package com.telemetry.reporting;

import com.google.gson.JsonParser;
import com.telemetry.integration.Session;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class ReportTest
{
    @TempDir
    Path folder;

    @Test
    void jsonIncludeStatistics() throws Exception
    {
        Session session = new Session();
        session.start("Demo");

        session.record("COMPILATION_STARTED", "", "");
        session.record("COMPILATION_SUCCESS", "", "");
        session.finish();

        Path file = folder.resolve("report.json");
        new JsonReport().export(session.snapshot(), file);
        var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

        assertEquals("TEST123", json.get("student_id").getAsString());
        assertEquals(1, json.get("total_compilations").getAsInt());
    }

    @Test 
    void pdfHasTwoPages() throws Exception
    {
        Session session = new Session();
        session.start("Demo");
        session.finish();

        Path file = folder.resolve("report.pdf");
        new PdfReport().export(session.snapshot(), file);

        try (PDDocument document = Loader.loadPDF(file.toFile()))
        {
            assertEquals(2, document.getNumberOfPages());
        }
    }

    @Test
    void factoryMethodBuildsBothExporters() throws Exception
    {
        assertInstanceOf(JsonReport.class, new ReportFactory.JsonFactory().createReport());
        assertInstanceOf(PdfReport.class, new ReportFactory.PdfFactory().createReport());
    }

    @Test 
    void jsonContainsErrorStatisticsAndEvents() throws Exception
    {
        Session session = new Session();
        session.start("Demo");

        session.record("COMPILATION_STARTED", "", "");
        session.record("COMPILATION_ERROR", "", "Line 4: missing ;");
        session.record("COMPILATION_FAILURE", "", "");
        session.finish();

        Path file = folder.resolve("reports/session.json");
        new JsonReport().export(session.snapshot(), file);
        var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

        assertEquals(1, json.get("failed_compilations").getAsInt());
        assertEquals(1, json.getAsJsonObject("error_frequencies").get("Line 4: missing ;").getAsInt());

        assertTrue(json.getAsJsonArray("events").size() >= 4);
        assertTrue(
            json.getAsJsonArray()
            .get(0)
            .getAsJsonObject()
            .get("timestamp")
            .getAsString()
            .contains("T"));
    }

    @Test 
    void jsonOfEmptySessionIsValid() throws Exception
    {
        Session session = new Session();
        session.start("Empty");
        session.finish();

        Path file = folder.resolve("empty.json");
        new JsonReport().export(session.snapshot(), file);
        var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();

        assertEquals(0, json.get("total_compilations").getAsInt());
        assertEquals(0.0, json.get("success_rate_percent").getAsDouble());
    }
}
