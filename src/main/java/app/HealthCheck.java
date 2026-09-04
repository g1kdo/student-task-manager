package app;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reports whether the application is actually healthy.
 *
 * <p>The previous health check printed the fixed string
 * {@code "Application is running."}, which was true by construction — it could
 * not fail, so it told the reader nothing. This one inspects live state: it
 * reads the task store (a store that throws is the failure this catches),
 * measures heap pressure, and reports uptime and task counts.
 *
 * <p>The clock and the heap probe are injected so a test can drive uptime and
 * simulate memory pressure deterministically instead of asserting on whatever
 * the JVM happens to be doing.
 */
public class HealthCheck {

    private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);

    /** Above this share of the maximum heap, the application reports DEGRADED. */
    static final double HEAP_DEGRADED_THRESHOLD_PERCENT = 90.0;

    /** Heap figures, injected so tests can simulate memory pressure. */
    public interface HeapProbe {
        long usedBytes();

        long maxBytes();

        /** @return a probe reading the live JVM heap */
        static HeapProbe live() {
            return new HeapProbe() {
                @Override
                public long usedBytes() {
                    Runtime runtime = Runtime.getRuntime();
                    return runtime.totalMemory() - runtime.freeMemory();
                }

                @Override
                public long maxBytes() {
                    return Runtime.getRuntime().maxMemory();
                }
            };
        }
    }

    private final TaskManager manager;
    private final Clock clock;
    private final HeapProbe heap;
    private final Instant startedAt;

    /** Creates a health check against the live clock and JVM heap. */
    public HealthCheck(TaskManager manager) {
        this(manager, Clock.systemUTC(), HeapProbe.live());
    }

    public HealthCheck(TaskManager manager, Clock clock, HeapProbe heap) {
        this.manager = manager;
        this.clock = clock;
        this.heap = heap;
        this.startedAt = clock.instant();
    }

    /**
     * Runs every check and combines the results.
     *
     * @return the current status; never throws, because a health check that
     *         fails by throwing cannot report that the application is unwell
     */
    public HealthStatus check() {
        Map<String, String> checks = new LinkedHashMap<>();
        HealthStatus.Status status = HealthStatus.Status.UP;

        int taskCount = -1;
        int completedTasks = -1;
        try {
            taskCount = manager.getTaskCount();
            completedTasks = manager.getCompletedCount();
            checks.put("taskStore", "readable, holding " + taskCount + " task(s)");
        } catch (RuntimeException e) {
            checks.put("taskStore", "unreadable: " + e);
            status = HealthStatus.Status.DOWN;
        }

        long usedHeap = heap.usedBytes();
        long maxHeap = heap.maxBytes();
        double heapPercent = maxHeap <= 0 ? 0.0 : (usedHeap * 100.0) / maxHeap;
        if (heapPercent >= HEAP_DEGRADED_THRESHOLD_PERCENT) {
            checks.put("heap", String.format("%.1f%% used, at or above the %.0f%% threshold",
                    heapPercent, HEAP_DEGRADED_THRESHOLD_PERCENT));
            // A full heap does not make the application DOWN, and it must not
            // downgrade a verdict that is already DOWN.
            if (status == HealthStatus.Status.UP) {
                status = HealthStatus.Status.DEGRADED;
            }
        } else {
            checks.put("heap", String.format("%.1f%% used", heapPercent));
        }

        Duration uptime = Duration.between(startedAt, clock.instant());
        HealthStatus result = new HealthStatus(
                status, uptime, taskCount, completedTasks, usedHeap, maxHeap,
                Collections.unmodifiableMap(checks));

        logResult(result);
        return result;
    }

    /** Logs at a level matching the verdict, so a bad result stands out in the log. */
    private void logResult(HealthStatus result) {
        switch (result.status()) {
            case UP -> log.info("Health check: {}", result);
            case DEGRADED -> log.warn("Health check reported DEGRADED: {} {}", result, result.checks());
            case DOWN -> log.error("Health check reported DOWN: {} {}", result, result.checks());
        }
    }
}
