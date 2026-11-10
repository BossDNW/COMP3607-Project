
package com.telemetry.reports;

import com.google.gson.JsonParser;
import com.telemetry.session.SessionManager;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ReportExporterCreatorTest {

    @TempDir
    Path tempDir;

    private SessionManager createSession() {
        SessionManager session = new SessionManager();
        session.startSession();
        session.endSession();
        return session;
    }

    @Test
    void jsonCreatorCreatesCorrectStrategy() {
        ReportExporterCreator creator = new JsonExporterCreator();

        ReportExporter exporter = creator.createExporter();

        assertInstanceOf(JsonExporter.class, exporter);
    }

    @Test
    void pdfCreatorCreatesCorrectStrategy() {
        ReportExporterCreator creator = new PdfExporterCreator();

        ReportExporter exporter = creator.createExporter();

        assertInstanceOf(PdfReportGenerator.class, exporter);
    }

    @Test
    void factoryExportsValidJson() throws Exception {
        ReportExporterCreator creator = new JsonExporterCreator();

        Path output = tempDir.resolve("factory-session.json");

        creator.exportReport(createSession(), output);

        assertTrue(Files.exists(output));

        var json = JsonParser.parseString(
                Files.readString(output)
        ).getAsJsonObject();

        assertEquals(0, json.get("total_compilations").getAsInt());
    }

    @Test
    void factoryExportsTwoPagePdf() throws Exception {
        ReportExporterCreator creator = new PdfExporterCreator();

        Path output = tempDir.resolve("factory-report.pdf");

        creator.exportReport(createSession(), output);

        assertTrue(Files.exists(output));

        try (PDDocument pdf = Loader.loadPDF(output.toFile())) {
            assertEquals(2, pdf.getNumberOfPages());
        }
    }
}
