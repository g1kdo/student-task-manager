package app;

import java.time.Duration;
import java.util.Map;

/**
 * The result of one {@link HealthCheck} run: an overall verdict plus the
 * measurements it was derived from, so a reader can see why the application
 * reported what it did.
 *
 * @param status         the overall verdict
 * @param uptime         how long the application has been running
 * @param taskCount      tasks currently held, or {@code -1} if unreadable
 * @param completedTasks tasks marked complete, or {@code -1} if unreadable
 * @param usedHeapBytes  heap currently in use
 * @param maxHeapBytes   heap the JVM is allowed to grow to
 * @param checks         one entry per individual check, in the order they ran
 */
public record HealthStatus(
        Status status,
        Duration uptime,
        int taskCount,
        int completedTasks,
        long usedHeapBytes,
        long maxHeapBytes,
        Map<String, String> checks) {

    /** UP: everything passed. DEGRADED: serving, but a check is unhappy. DOWN: a check failed. */
    public enum Status { UP, DEGRADED, DOWN }

    public boolean isHealthy() {
        return status == Status.UP;
    }

    /** @return heap in use as a percentage of the maximum, or 0 if unknown */
    public double heapUsedPercent() {
        return maxHeapBytes <= 0 ? 0.0 : (usedHeapBytes * 100.0) / maxHeapBytes;
    }

    /** A single line suitable for the console and for a log record. */
    @Override
    public String toString() {
        return String.format(
                "status=%s uptime=%s tasks=%d completed=%d heap=%.1fMB/%.1fMB (%.1f%%)",
                status,
                formatUptime(uptime),
                taskCount,
                completedTasks,
                usedHeapBytes / (1024.0 * 1024.0),
                maxHeapBytes / (1024.0 * 1024.0),
                heapUsedPercent());
    }

    private static String formatUptime(Duration uptime) {
        return String.format("%dh%02dm%02ds",
                uptime.toHours(), uptime.toMinutesPart(), uptime.toSecondsPart());
    }
}
