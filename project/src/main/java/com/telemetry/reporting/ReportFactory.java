package com.telemetry.reporting;

public abstract class ReportFactory {
    public abstract ReportExporter createReport();
    
    public static class JsonFactory extends ReportFactory {
        @Override
        public ReportExporter createReport() {
            return new JsonReport();
        }
    }

    public static class PdfFactory extends ReportFactory {
        @Override 
        public ReportExporter createReport() {
            return new PdfReport();
        }
    }
}
