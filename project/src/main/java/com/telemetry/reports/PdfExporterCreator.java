package com.telemetry.reports;

public class PdfExporterCreator extends ReportExporterCreator {

    @Override
    public ReportExporter createExporter() {
        return new PdfReportGenerator();
    }
}
