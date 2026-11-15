package com.telemetry.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.telemetry.integration.Session;

public class EditMonitor {

    private final Map<Path, Map<Path, Long>> watched= new HashMap<>();

    //Record initial source-file modification times
    public synchronized void watch(Path folder){
        watched.put(folder, scan(folder));
    }

    //Compare saved-file times against the previous scan
    public synchronized void check(Session session){
        for (Path folder : watched.keySet()){
            Map<Path, Long> before = watched.get(folder);
            Map<Path, Long> after = scan(folder);

            for (Map.Entry<Path, Long> file: after.entrySet()){
                Long previous = before.get(file.getKey());

                if (previous != null && file.getValue() > previous){
                    session.record("EDIT_ACTIVITY", file.getKey().getFileName().toString()), "Saved source file");
                }
            }

            watched.put(folder, after);
        }
    }

}
