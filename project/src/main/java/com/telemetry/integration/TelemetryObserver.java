package com.telemetry.integration;

public interface TelemetryObserver {
    void update(TelemetryEvent event);
}
