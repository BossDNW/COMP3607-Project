package com.telemetry.reporting;

import java.io.IOException;
import java.nio.file.Path;
import com.telemetry.integration.Session;

public interface ReportExporter {
    void export(Session session, Path output) throws IOException;
}
