
package com.telemetry.reports;

import com.telemetry.session.SessionManager;

import java.io.IOException;
import java.nio.file.Path;

public interface ReportExporter {

    void export(SessionManager session, Path output)
            throws IOException;
}
