package com.telemetry.integration;

import java.time.LocalDateTime;

public class TelemetryEvent {
    private final String type;
    private final LocalDateTime time;
    private final String name;
    private final String details;

    public TelemetryEvent(String type, LocalDateTime time, String name, String details){
        this.type = type;
        this.time = time;
        this.name = name;
        this.details = details;
    }

    public String getType(){
        return type;
    }

    public LocalDateTime getTime(){
        return time;
    }
    
    public String getName(){
        return name;
    }

    public String getDetails(){
        return details;
    }
}
