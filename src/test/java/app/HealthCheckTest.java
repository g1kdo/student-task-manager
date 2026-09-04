package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link HealthCheck}.
 *
 * <p>The clock and heap probe are supplied by the test, so DEGRADED and DOWN
 * are asserted directly rather than hoped for.
 */
class HealthCheckTest {

    private static final long ONE_GIB = 1024L * 1024 * 1024;

    /** A heap probe reporting fixed figures. */
    private static HealthCheck.HeapProbe heapAt(long used, long max) {
        return new HealthCheck.HeapProbe() {
            @Override
            public long usedBytes() {
                return used;
            }

            @Override
            public long maxBytes() {
                return max;
            }
        };
    }

    /** A clock that starts at a fixed instant and can be advanced by the test. */
    private static final class TickingClock extends Clock {
        private Instant now = Instant.parse("2026-09-04T09:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    @DisplayName("a healthy application reports UP with its task counts")
    void healthyApplicationReportsUp() {
        TaskManager manager = new TaskManager();
        manager.addTask("Revise");
        manager.addTask("Submit");
        manager.completeTask(0);

        HealthStatus health = new HealthCheck(manager, new TickingClock(), heapAt(ONE_GIB / 4, ONE_GIB))
                .check();

        assertEquals(HealthStatus.Status.UP, health.status());
        assertTrue(health.isHealthy());
        assertEquals(2, health.taskCount());
        assertEquals(1, health.completedTasks());
        assertEquals("readable, holding 2 task(s)", health.checks().get("taskStore"));
    }

    @Test
    @DisplayName("uptime is measured from construction to the check")
    void uptimeIsMeasuredFromConstruction() {
        TickingClock clock = new TickingClock();
        HealthCheck check = new HealthCheck(new TaskManager(), clock, heapAt(0, ONE_GIB));

        clock.advance(Duration.ofMinutes(90).plusSeconds(5));

        assertEquals(Duration.ofMinutes(90).plusSeconds(5), check.check().uptime());
    }

    @Test
    @DisplayName("heap at or above the threshold reports DEGRADED")
    void heapPressureReportsDegraded() {
        HealthStatus health = new HealthCheck(
                new TaskManager(), new TickingClock(),
                heapAt((long) (ONE_GIB * 0.95), ONE_GIB)).check();

        assertEquals(HealthStatus.Status.DEGRADED, health.status());
        assertFalse(health.isHealthy());
        assertTrue(health.checks().get("heap").contains("above the 90% threshold"),
                "the reason should say which threshold was crossed");
    }

    @Test
    @DisplayName("heap just below the threshold still reports UP")
    void heapJustBelowThresholdStaysUp() {
        HealthStatus health = new HealthCheck(
                new TaskManager(), new TickingClock(),
                heapAt((long) (ONE_GIB * 0.899), ONE_GIB)).check();

        assertEquals(HealthStatus.Status.UP, health.status());
    }

    @Test
    @DisplayName("an unreadable task store reports DOWN without throwing")
    void unreadableTaskStoreReportsDown() {
        TaskManager broken = new TaskManager() {
            @Override
            public int getTaskCount() {
                throw new IllegalStateException("task store unavailable");
            }
        };

        HealthStatus health =
                new HealthCheck(broken, new TickingClock(), heapAt(0, ONE_GIB)).check();

        assertEquals(HealthStatus.Status.DOWN, health.status());
        assertEquals(-1, health.taskCount(), "an unreadable store reports -1, not a made-up 0");
        assertTrue(health.checks().get("taskStore").contains("task store unavailable"));
    }

    @Test
    @DisplayName("heap pressure does not soften a DOWN verdict to DEGRADED")
    void downIsNotDowngradedByHeapPressure() {
        TaskManager broken = new TaskManager() {
            @Override
            public int getTaskCount() {
                throw new IllegalStateException("task store unavailable");
            }
        };

        HealthStatus health = new HealthCheck(
                broken, new TickingClock(), heapAt((long) (ONE_GIB * 0.95), ONE_GIB)).check();

        assertEquals(HealthStatus.Status.DOWN, health.status());
    }

    @Test
    @DisplayName("an unknown maximum heap is reported as 0% rather than dividing by zero")
    void unknownMaxHeapDoesNotDivideByZero() {
        HealthStatus health =
                new HealthCheck(new TaskManager(), new TickingClock(), heapAt(1024, 0)).check();

        assertEquals(0.0, health.heapUsedPercent());
        assertEquals(HealthStatus.Status.UP, health.status());
    }

    @Test
    @DisplayName("the summary line carries the fields a reader or a CI grep needs")
    void summaryLineCarriesTheKeyFields() {
        TaskManager manager = new TaskManager();
        manager.addTask("Revise");

        String summary = new HealthCheck(manager, new TickingClock(), heapAt(ONE_GIB / 2, ONE_GIB))
                .check().toString();

        assertTrue(summary.contains("status=UP"), summary);
        assertTrue(summary.contains("tasks=1"), summary);
        assertTrue(summary.contains("completed=0"), summary);
        assertTrue(summary.contains("uptime=0h00m00s"), summary);
    }

    @Test
    @DisplayName("the per-check detail map cannot be modified by a caller")
    void checksMapIsUnmodifiable() {
        HealthStatus health =
                new HealthCheck(new TaskManager(), new TickingClock(), heapAt(0, ONE_GIB)).check();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> health.checks().put("injected", "value"));
    }
}
