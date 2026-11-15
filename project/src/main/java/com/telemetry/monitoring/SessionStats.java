package com.telemetry.monitoring;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public int getAttempts(){
        return attempts;
    }

    public int getSuccesses(){
        return successes;
    }

    public int getFailures(){
        return failures;
    }

    public int getEdits(){
        return edits;
    }

    public int getRuns(){
        return runs;
    }

    public int getRuntimeErrors(){
        return runtimeErrors;
    }

    public Map<String, Integer> getErrors(){
        return new LinkedHashMap<>(errors);
    }

    public double getSuccessRate(){
        return attempts == 0 ? 0 : 100.0 * successes/attempts;
    }

    public List<Long> getCompileIntervals(){
        List<Long> intervals = new ArrayList<>();

        for (int i=1; i<compileTimes.size(); i++){
            intervals.add(Duration.between(compileTimes.get(i-1), compileTimes.get(i)).getSeconds());
        }
        return intervals;
    }

    //Estimates idle seconds using a 180-second threshold.
    public static long idleSeconds(Session session){
        LocalDateTime previous = session.getStart();
        LocalDateTime end = session.getEnd();

        if (previous == null || end == null){
            return 0;
        }

        long idle= 0;

        for (TelemetryEvent event : session.getEvents()){
            long gap= Duration.between(previous, event.getTime()).getSeconds();

            idle += Math.max(0, gap-180);
            previous = event.getTime();
        }

        long lastGap= Duration.between(previous, end).getSeconds();
        idle += Math.max(0, lastGap-180);

        return Math.min(session.durationSeconds(), idle);
    }

    public static long activeSeconds(Session session){
        return session.durationSeconds()-idleSeconds(session);
    }

}
