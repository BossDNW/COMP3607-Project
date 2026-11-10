package com.telemetry.reports;

public class JsonExporterCreator extends ReportExporterCreator {

    @Override
    public ReportExporter createExporter() {
        return new JsonExporter();
    }
}
