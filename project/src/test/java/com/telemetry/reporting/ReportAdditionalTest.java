package com.telemetry.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;
import com.telemetry.integration.Session;

public class ReportAdditionalTest
{
    @TempDir 
    Path folder;

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
