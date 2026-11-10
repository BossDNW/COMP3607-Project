package com.telemetry.events;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TelemetryEventBusTest {

    private TelemetryEvent testEvent() {
        return new TelemetryEvent(
                EventType.CLASS_CREATED,
                LocalDateTime.now(),
                "Test",
                "HelloWorld",
                Map.of()
        );
    }

    @Test
    void subscribedObserverReceivesEvent() {
        TelemetryEventBus bus = new TelemetryEventBus();
        AtomicInteger count = new AtomicInteger();

        bus.subscribe(event -> count.incrementAndGet());
        bus.publish(testEvent());

        assertEquals(1, count.get());
    }

    @Test
    void multipleObserversReceiveEvent() {
        TelemetryEventBus bus = new TelemetryEventBus();

        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();

        bus.subscribe(event -> first.incrementAndGet());
        bus.subscribe(event -> second.incrementAndGet());

        bus.publish(testEvent());

        assertEquals(1, first.get());
        assertEquals(1, second.get());
    }

    @Test
    void unsubscribedObserverStopsReceivingEvents() {
        TelemetryEventBus bus = new TelemetryEventBus();
        AtomicInteger count = new AtomicInteger();

        TelemetryRecorder recorder =
                event -> count.incrementAndGet();

        bus.subscribe(recorder);
        bus.publish(testEvent());

        bus.unsubscribe(recorder);
        bus.publish(testEvent());

        assertEquals(1, count.get());
    }

    @Test
    void subscribingTwiceDoesNotDuplicateNotifications() {
        TelemetryEventBus bus = new TelemetryEventBus();
        AtomicInteger count = new AtomicInteger();

        TelemetryRecorder recorder =
                event -> count.incrementAndGet();

        bus.subscribe(recorder);
        bus.subscribe(recorder);

        bus.publish(testEvent());

        assertEquals(1, count.get());
    }
}
