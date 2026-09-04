package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link TaskManager}.
 *
 * <p>The earlier version of this class asserted only that adding one task and
 * completing it worked. These tests cover the paths that were previously
 * unexercised: listing, deleting, out-of-range task numbers, blank names and
 * repeated completion.
 */
class TaskManagerTest {

    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new TaskManager();
    }

    @Nested
    @DisplayName("US1 - Add a task")
    class AddTask {

        @Test
        @DisplayName("a task is stored and the count reflects it")
        void addStoresTheTask() {
            assertTrue(manager.addTask("Study"), "adding a valid task should report success");

            assertEquals(1, manager.getTaskCount());
            assertEquals("Study", manager.getTasks().get(0).getName());
            assertFalse(manager.getTasks().get(0).isCompleted(), "a new task starts pending");
        }

        @Test
        @DisplayName("tasks are kept in the order they were added")
        void addPreservesInsertionOrder() {
            manager.addTask("First");
            manager.addTask("Second");
            manager.addTask("Third");

            assertEquals(List.of("First", "Second", "Third"),
                    manager.getTasks().stream().map(Task::getName).toList());
        }

        @ParameterizedTest(name = "a name of [{0}] is rejected")
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        @DisplayName("a blank name is rejected instead of stored")
        void addRejectsBlankNames(String blank) {
            assertFalse(manager.addTask(blank), "a blank name should be rejected");
            assertEquals(0, manager.getTaskCount(), "nothing should have been stored");
        }

        @Test
        @DisplayName("a null name is rejected instead of throwing")
        void addRejectsNull() {
            assertFalse(manager.addTask(null));
            assertEquals(0, manager.getTaskCount());
        }

        @Test
        @DisplayName("surrounding whitespace is trimmed from the name")
        void addTrimsTheName() {
            manager.addTask("  Revise chapter 4  ");

            assertEquals("Revise chapter 4", manager.getTasks().get(0).getName());
        }

        @Test
        @DisplayName("the overloaded add reports invalid contract values")
        void addReportsInvalidValues() {
            assertEquals(AddResult.REJECTED_INVALID, manager.addTask(null, null, null));
            assertEquals(AddResult.REJECTED_INVALID, manager.addTask("   ", null, null));
            assertEquals(AddResult.REJECTED_INVALID,
                    manager.addTask("a".repeat(121), null, null));
            assertEquals(AddResult.REJECTED_INVALID,
                    manager.addTask("Reminder", null, 1));
            assertEquals(AddResult.REJECTED_INVALID,
                    manager.addTask("Reminder", LocalDate.of(2026, 9, 30), 31));
            assertEquals(AddResult.ADDED,
                    manager.addTask("Valid", LocalDate.of(2026, 9, 30), 0));
        }

        @Test
        @DisplayName("duplicates ignore case and surrounding whitespace")
        void duplicateNamesAreRejected() {
            assertEquals(AddResult.ADDED, manager.addTask(" Revise Maths ", null, null));
            assertEquals(AddResult.REJECTED_DUPLICATE,
                    manager.addTask("revise maths", LocalDate.of(2026, 9, 30), null));
            assertEquals(1, manager.getTaskCount());
            assertTrue(manager.hasTaskNamed("  REVISE MATHS "));
        }

        @Test
        @DisplayName("a deleted name can be added again")
        void deletedNameCanBeReused() {
            manager.addTask("Revise");
            manager.deleteTask(0);

            assertEquals(AddResult.ADDED, manager.addTask("revise", null, null));
        }

        @Test
        @DisplayName("reminders are returned in due-date order")
        void remindersAreSortedByDueDate() {
            // Both tasks must actually be inside their reminder window on the
            // date under test, or this asserts filtering rather than ordering.
            // "Later" is due 2026-10-02 with 4 days' lead, so its window is
            // [2026-09-28, 2026-10-02] and includes the date below; "Sooner" is
            // due 2026-09-28 with 3 days' lead, window [2026-09-25, 2026-09-28].
            manager.addTask("Later", LocalDate.of(2026, 10, 2), 4);
            manager.addTask("Sooner", LocalDate.of(2026, 9, 28), 3);

            assertEquals(List.of("Sooner", "Later"),
                    manager.tasksNeedingReminder(LocalDate.of(2026, 9, 28))
                            .stream().map(Task::getName).toList());
        }

        @Test
        @DisplayName("a task outside its reminder window is filtered out")
        void remindersExcludeTasksOutsideTheirWindow() {
            // The case the ordering test above was accidentally asserting.
            manager.addTask("Not yet", LocalDate.of(2026, 10, 2), 2);

            assertTrue(manager.tasksNeedingReminder(LocalDate.of(2026, 9, 28)).isEmpty(),
                    "2026-09-28 is before the [2026-09-30, 2026-10-02] window");
        }
    }

    @Nested
    @DisplayName("US2 - View tasks")
    class ViewTasks {

        @Test
        @DisplayName("an empty manager reports an empty list, not an error")
        void emptyManagerReturnsEmptyList() {
            assertTrue(manager.getTasks().isEmpty());
            assertEquals(0, manager.getTaskCount());
            assertEquals(0, manager.getCompletedCount());
        }

        @Test
        @DisplayName("the list reports completion state per task")
        void listReportsCompletionState() {
            manager.addTask("Done one");
            manager.addTask("Still pending");
            manager.completeTask(0);

            List<Task> tasks = manager.getTasks();
            assertTrue(tasks.get(0).isCompleted());
            assertFalse(tasks.get(1).isCompleted());
            assertEquals(1, manager.getCompletedCount());
        }

        @Test
        @DisplayName("the returned list cannot be modified behind the manager's back")
        void listIsUnmodifiable() {
            manager.addTask("Study");

            assertThrows(UnsupportedOperationException.class,
                    () -> manager.getTasks().add(new Task("Sneaked in")));
        }
    }

    @Nested
    @DisplayName("US3 - Complete a task")
    class CompleteTask {

        @Test
        @DisplayName("completing an existing task marks it done")
        void completeMarksTheTaskDone() {
            manager.addTask("Revise");

            assertTrue(manager.completeTask(0));
            assertTrue(manager.isTaskCompleted(0));
            assertEquals(1, manager.getCompletedCount());
        }

        @Test
        @DisplayName("completing the same task twice is reported as no change")
        void completeIsIdempotent() {
            manager.addTask("Revise");
            manager.completeTask(0);

            assertFalse(manager.completeTask(0), "the second call changes nothing");
            assertEquals(1, manager.getCompletedCount(), "the count must not double");
        }

        @ParameterizedTest(name = "index {0} is rejected")
        @ValueSource(ints = {-1, 1, 2, 99, Integer.MIN_VALUE, Integer.MAX_VALUE})
        @DisplayName("an out-of-range task number is rejected rather than throwing")
        void completeRejectsOutOfRangeIndexes(int index) {
            manager.addTask("The only task");

            assertFalse(manager.completeTask(index));
            assertFalse(manager.isTaskCompleted(0), "the real task must be untouched");
        }

        @Test
        @DisplayName("completing anything in an empty manager is rejected")
        void completeOnEmptyManagerIsRejected() {
            assertFalse(manager.completeTask(0));
        }
    }

    @Nested
    @DisplayName("US4 - Delete a task")
    class DeleteTask {

        @Test
        @DisplayName("deleting removes the task and shifts the rest up")
        void deleteRemovesTheTask() {
            manager.addTask("First");
            manager.addTask("Second");
            manager.addTask("Third");

            assertTrue(manager.deleteTask(1));

            assertEquals(2, manager.getTaskCount());
            assertEquals(List.of("First", "Third"),
                    manager.getTasks().stream().map(Task::getName).toList());
        }

        @Test
        @DisplayName("deleting a completed task lowers the completed count")
        void deleteUpdatesTheCompletedCount() {
            manager.addTask("Done");
            manager.completeTask(0);
            assertEquals(1, manager.getCompletedCount());

            manager.deleteTask(0);

            assertEquals(0, manager.getCompletedCount());
        }

        @ParameterizedTest(name = "index {0} is rejected")
        @ValueSource(ints = {-1, 1, 5, Integer.MIN_VALUE, Integer.MAX_VALUE})
        @DisplayName("an out-of-range task number is rejected rather than throwing")
        void deleteRejectsOutOfRangeIndexes(int index) {
            manager.addTask("The only task");

            assertFalse(manager.deleteTask(index));
            assertEquals(1, manager.getTaskCount(), "nothing should have been removed");
        }

        @Test
        @DisplayName("deleting from an empty manager is rejected")
        void deleteOnEmptyManagerIsRejected() {
            assertFalse(manager.deleteTask(0));
        }

        @Test
        @DisplayName("every task can be deleted, leaving an empty manager")
        void deletingEverythingLeavesAnEmptyManager() {
            manager.addTask("One");
            manager.addTask("Two");

            assertTrue(manager.deleteTask(0));
            assertTrue(manager.deleteTask(0));

            assertEquals(0, manager.getTaskCount());
            assertTrue(manager.getTasks().isEmpty());
        }
    }
}
