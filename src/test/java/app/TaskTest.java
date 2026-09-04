package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for the {@link Task} model and the invariant it guards. */
class TaskTest {

    @Test
    @DisplayName("a new task keeps its name and starts pending")
    void newTaskStartsPending() {
        Instant before = Instant.now();

        Task task = new Task("Read chapter 3");

        assertEquals("Read chapter 3", task.getName());
        assertFalse(task.isCompleted());
        assertFalse(task.getCreatedAt().isBefore(before), "createdAt should be set at construction");
    }

    @ParameterizedTest(name = "a name of [{0}] is refused")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n", "   "})
    @DisplayName("a blank name is refused at construction")
    void blankNameIsRefused(String blank) {
        IllegalArgumentException thrown =
                assertThrows(IllegalArgumentException.class, () -> new Task(blank));

        assertEquals("Task name must not be blank", thrown.getMessage());
    }

    @Test
    @DisplayName("surrounding whitespace is trimmed")
    void nameIsTrimmed() {
        assertEquals("Submit report", new Task("   Submit report \t ").getName());
    }

    @Test
    @DisplayName("completing a pending task reports the change")
    void completeReportsTheChange() {
        Task task = new Task("Revise");

        assertTrue(task.complete(), "the first completion is a real change");
        assertTrue(task.isCompleted());
    }

    @Test
    @DisplayName("completing an already-complete task reports no change")
    void completeIsIdempotent() {
        Task task = new Task("Revise");
        task.complete();

        assertFalse(task.complete(), "the second completion changes nothing");
        assertTrue(task.isCompleted(), "and leaves the task complete");
    }

    @Test
    @DisplayName("toString shows the name and the status")
    void toStringShowsStatus() {
        Task task = new Task("Revise");

        assertEquals("Revise - Pending", task.toString());

        task.complete();

        assertEquals("Revise - Done", task.toString());
    }
}
