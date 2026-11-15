package com.telemetry.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.telemetry.integration.TelemetryEvent;


public class SessionStats implements Consumer<TelemetryEvent> {
    private int attempts;
    private int successes;
    private int failures;
    private int edits;
    private int runs;
    private int runtimeErrors;

    private final List<LocalDateTime> compileTimes= new ArrayList<>();
    private final Map<String, Integer> errors= new LinkedHashMap<>();
    private final Map<String, Consumer<TelementryEvent>> handlers= new HashMap<>();

    public SessionStats(){
        handlers.put("COMPILATION_STARTED", event -> {
            attempts++;
            compileTimes.add(event.getTime());
        });

        handlers.put("COMPILATION_SUCCESS", event -> {
            successes++;
        });

        handlers.put("COMPILATION_FAILURE", event -> {
            failures++;
        });

        handlers.put("COMPILATION_ERROR", event -> {
            errors.merge(event.getDetails(), 1, Integer::sum);
        });

        handlers.put ("EDIT_ACTIVITY", event -> {
            edits++;
        });

        handlers.put("PROGRAM_EXCUTED", event -> {
            runs++;
        });

        handlers.put("RUNTIME_ERROR", event -> {
            runtimeErrors++;
        });

    }

}
