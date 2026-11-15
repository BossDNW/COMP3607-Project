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
    private final Map<String, Consumer<TelemetryEvent>> handlers= new HashMap<>();

    @Override
    public void accept (TelemetryEvent event){
        switch (event.getType()){

            case "COMPILATION_STARTED":
                attempts++;
                compileTimes.add(event.getTime());
                break;

            case "COMPILATION_SUCCESS":
                successes++;
                break;
            
            case "COMPILATION_FAILURE":
                failures++;
                break;
            
            case "COMPILATION_ERROR":
                errors.merge(event.getDetails(), 1, Integer::sum);
                break;
            
            case "EDIT_ACTIVITY":
                edits++;
                break;
            
            case "PROGRAM_EXECUTED":
                runs++;
                break;
            
            case "RUNTIME_ERROR":
                runtimeErrors++;
                break;
            
            default:
                break;
        }
    }

    
}
