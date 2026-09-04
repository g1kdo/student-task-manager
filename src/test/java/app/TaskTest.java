package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
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
    @DisplayName("due date and reminder lead time are stored when provided")
    void optionalFieldsAreStored() {
        Task task = new Task("Revise", LocalDate.of(2026, 9, 30), 3);

        assertEquals(Optional.of(LocalDate.of(2026, 9, 30)), task.getDueDate());
        assertEquals(Optional.of(3), task.getRemindBeforeDays());
    }

    @Test
    @DisplayName("a name longer than 120 characters is refused")
    void longNameIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new Task("a".repeat(121)));
    }

    @Test
    @DisplayName("a reminder requires a due date")
    void reminderRequiresDueDate() {
        assertThrows(IllegalArgumentException.class, () -> new Task("Revise", null, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 31})
    @DisplayName("a reminder lead time outside 0 to 30 is refused")
    void reminderLeadTimeMustBeBounded(int days) {
        assertThrows(IllegalArgumentException.class,
                () -> new Task("Revise", LocalDate.of(2026, 9, 30), days));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 30})
    @DisplayName("a reminder lead time from 0 to 30 is accepted")
    void reminderLeadTimeBoundariesAreAccepted(int days) {
        assertEquals(Optional.of(days),
                new Task("Revise", LocalDate.of(2026, 9, 30), days).getRemindBeforeDays());
    }

    @Test
    @DisplayName("urgency is derived from the date supplied by the caller")
    void urgencyIsDerivedFromToday() {
        Task task = new Task("Revise", LocalDate.of(2026, 9, 30), null);

        assertEquals(Urgency.DUE_TODAY, task.urgencyOn(LocalDate.of(2026, 9, 30)));
        assertEquals(Urgency.OVERDUE, task.urgencyOn(LocalDate.of(2026, 10, 1)));
        assertEquals(Urgency.UPCOMING, task.urgencyOn(LocalDate.of(2026, 9, 29)));
        assertEquals(Urgency.NONE, new Task("No date").urgencyOn(LocalDate.of(2026, 9, 30)));
    }

    @Test
    @DisplayName("reminders use inclusive boundaries and ignore completed tasks")
    void remindersUseInclusiveBoundaries() {
        Task task = new Task("Revise", LocalDate.of(2026, 9, 30), 3);

        assertTrue(task.needsReminderOn(LocalDate.of(2026, 9, 27)));
        assertFalse(task.needsReminderOn(LocalDate.of(2026, 9, 26)));
        assertTrue(task.needsReminderOn(LocalDate.of(2026, 9, 30)));
        assertFalse(task.needsReminderOn(LocalDate.of(2026, 10, 1)));
        task.complete();
        assertFalse(task.needsReminderOn(LocalDate.of(2026, 9, 30)));

        Task sameDay = new Task("Submit", LocalDate.of(2026, 9, 30), 0);
        assertFalse(sameDay.needsReminderOn(LocalDate.of(2026, 9, 29)));
        assertTrue(sameDay.needsReminderOn(LocalDate.of(2026, 9, 30)));
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
