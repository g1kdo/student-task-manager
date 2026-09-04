package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link ConsoleApp}: each one drives the real menu loop
 * with scripted keystrokes and asserts on what the application printed and on
 * the state it left behind.
 *
 * <p>These cover the layer that shipped the reported bug — menu handling and
 * input parsing — which no test previously touched.
 */
class ConsoleAppTest {

    /** One completed session. */
    private record Session(String output, TaskManager manager) {
        boolean printed(String text) {
            return output.contains(text);
        }
    }

    /**
     * Runs the menu with {@code keystrokes} as stdin.
     *
     * <p>Wrapped in a timeout: a menu loop that mishandles end of input spins
     * forever, and this should fail the build rather than hang it.
     */
    private static Session run(String... keystrokes) {
        return runAt(Clock.systemDefaultZone(), keystrokes);
    }

    private static Session runAt(Clock clock, String... keystrokes) {
        String input = String.join("\n", keystrokes) + "\n";
        TaskManager manager = new TaskManager();
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                new ConsoleApp(
                        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                        new PrintStream(captured, true, StandardCharsets.UTF_8),
                        manager, new HealthCheck(manager), clock).run());

        return new Session(captured.toString(StandardCharsets.UTF_8), manager);
    }

    // ---------------------------------------------------------------------
    // Regression test for the reported bug.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("REGRESSION: completing a task does not make the next menu read fail")
    void completingATaskDoesNotBreakTheNextMenuRead() {
        // The old code read the task number with Scanner.nextInt(), which left
        // the newline in the buffer; the loop's next nextLine() then returned
        // an empty string and the menu printed 'Input a valid digit'.
        Session session = run("1", "Revise", "", "", "3", "1", "2", "6");

        assertFalse(session.printed("is not a valid choice"),
                "the menu must not reject the input that follows a task number");
        assertTrue(session.printed("Task completed."));
        assertTrue(session.printed("1 of 1 complete."),
                "the view that follows the completion must still be reachable");
        assertTrue(session.printed("Goodbye."), "and option 6 must still exit cleanly");
    }

    @Test
    @DisplayName("REGRESSION: deleting a task does not make the next menu read fail")
    void deletingATaskDoesNotBreakTheNextMenuRead() {
        Session session = run("1", "Revise", "", "", "4", "1", "2", "6");

        assertFalse(session.printed("is not a valid choice"));
        assertTrue(session.printed("Task deleted."));
        assertTrue(session.printed("You have no tasks yet."));
        assertTrue(session.printed("Goodbye."));
    }

    // ---------------------------------------------------------------------
    // Menu behaviour
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("the menu lists all six options")
    void menuListsEveryOption() {
        Session session = run("6");

        for (String option : new String[]{
                "1. Add Task", "2. View Tasks", "3. Complete Task",
                "4. Delete Task", "5. Health Check", "6. Exit"}) {
            assertTrue(session.printed(option), "missing menu entry: " + option);
        }
    }

    @Test
    @DisplayName("a non-numeric choice is reported and the loop continues")
    void nonNumericChoiceIsReported() {
        Session session = run("banana", "6");

        assertTrue(session.printed("'banana' is not a valid choice."));
        assertTrue(session.printed("Goodbye."), "the loop must survive bad input");
    }

    @Test
    @DisplayName("a numeric choice outside the menu is reported")
    void outOfRangeChoiceIsReported() {
        Session session = run("99", "6");

        assertTrue(session.printed("'99' is not a valid choice."));
        assertTrue(session.printed("Goodbye."));
    }

    @Test
    @DisplayName("a bare Enter redraws the menu without complaining")
    void blankChoiceIsIgnored() {
        Session session = run("", "6");

        assertFalse(session.printed("is not a valid choice"),
                "pressing Enter is not an error worth reporting");
        assertTrue(session.printed("Goodbye."));
    }

    @Test
    @DisplayName("the loop exits cleanly when the input ends without an explicit exit")
    void endOfInputExitsCleanly() {
        Session session = run("1", "Revise", "", "");

        assertTrue(session.printed("Input ended. Exiting."));
        assertEquals(1, session.manager().getTaskCount(), "the work done before EOF stands");
    }

    // ---------------------------------------------------------------------
    // US1 - Add
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("adding a task confirms it and stores it")
    void addingATaskConfirmsAndStores() {
        Session session = run("1", "Finish assessment", "", "", "6");

        assertTrue(session.printed("Task added."));
        assertEquals(1, session.manager().getTaskCount());
        assertEquals("Finish assessment", session.manager().getTasks().get(0).getName());
    }

    @Test
    @DisplayName("an empty task name is refused and nothing is stored")
    void emptyTaskNameIsRefused() {
        Session session = run("1", "   ", "", "", "6");

        assertTrue(session.printed("A task needs a name. Nothing was added."));
        assertEquals(0, session.manager().getTaskCount());
    }

    // ---------------------------------------------------------------------
    // US2 - View
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("viewing with no tasks says so instead of printing an empty list")
    void viewingAnEmptyListSaysSo() {
        assertTrue(run("2", "6").printed("You have no tasks yet."));
    }

    @Test
    @DisplayName("the list numbers tasks from one and shows each status")
    void listShowsNumbersAndStatuses() {
        Session session = run("1", "Alpha", "", "", "1", "Beta", "", "", "3", "1", "2", "6");

        assertTrue(session.printed("1. Alpha"));
        assertTrue(session.printed("2. Beta"));
        assertTrue(session.printed("[Done]"));
        assertTrue(session.printed("[Pending]"));
        assertTrue(session.printed("1 of 2 complete."));
    }

    @Test
    @DisplayName("adding optional dates and viewing tasks shows derived urgency")
    void optionalFieldsAndUrgencyAreShown() {
        Session session = runAt(
                Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC),
                "1", "Due today", "2026-09-30", "0", "2", "6");

        assertEquals("Due today", session.manager().getTasks().get(0).getName());
        assertEquals("2026-09-30",
                session.manager().getTasks().get(0).getDueDate().orElseThrow().toString());
        assertTrue(session.printed("DUE_TODAY"));
    }

    // ---------------------------------------------------------------------
    // US3 / US4 - Complete and delete
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("completing with no tasks says so and never asks for a number")
    void completingWithNoTasksSaysSo() {
        Session session = run("3", "6");

        assertTrue(session.printed("You have no tasks yet."));
        assertFalse(session.printed("Task number:"),
                "there is nothing to number, so the prompt should be skipped");
    }

    @Test
    @DisplayName("an out-of-range task number is reported without touching the task")
    void outOfRangeTaskNumberIsReported() {
        Session session = run("1", "Revise", "", "", "3", "9", "6");

        assertTrue(session.printed("Task 9 was not found"));
        assertFalse(session.manager().isTaskCompleted(0), "the real task is untouched");
    }

    @Test
    @DisplayName("a non-numeric task number is reported and the loop continues")
    void nonNumericTaskNumberIsReported() {
        Session session = run("1", "Revise", "", "", "4", "abc", "2", "6");

        assertTrue(session.printed("'abc' is not a task number."));
        assertEquals(1, session.manager().getTaskCount(), "nothing was deleted");
        assertTrue(session.printed("Goodbye."));
    }

    @Test
    @DisplayName("task number zero is reported rather than wrapping to the last task")
    void taskNumberZeroIsReported() {
        Session session = run("1", "Revise", "", "", "3", "0", "6");

        assertTrue(session.printed("Task 0 was not found"));
        assertFalse(session.manager().isTaskCompleted(0));
    }

    @Test
    @DisplayName("completing the same task twice reports it, and the count does not double")
    void completingTwiceIsReported() {
        Session session = run("1", "Revise", "", "", "3", "1", "3", "1", "2", "6");

        assertTrue(session.printed("already complete"));
        assertEquals(1, session.manager().getCompletedCount());
        assertTrue(session.printed("1 of 1 complete."));
    }

    @Test
    @DisplayName("deleting removes the task and renumbers the remainder")
    void deletingRenumbersTheRemainder() {
        Session session = run("1", "Alpha", "", "", "1", "Beta", "", "", "4", "1", "2", "6");

        assertTrue(session.printed("Task deleted."));
        assertEquals(1, session.manager().getTaskCount());
        assertTrue(session.printed("1. Beta"), "Beta becomes task 1");
    }

    // ---------------------------------------------------------------------
    // US5 - Health check
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("the health check reports live state, not a fixed string")
    void healthCheckReportsLiveState() {
        Session session = run("1", "Revise", "", "", "1", "Submit", "", "",
                "3", "1", "5", "6");

        assertTrue(session.printed("status=UP"));
        assertTrue(session.printed("tasks=2"), "the reported count must follow real state");
        assertTrue(session.printed("completed=1"));
        assertTrue(session.printed("taskStore"), "the individual checks should be shown");
        assertTrue(session.printed("heap"));
        assertFalse(session.printed("Application is running."),
                "the old fixed string should be gone");
    }

    // ---------------------------------------------------------------------
    // A longer session, end to end
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("a full session across all five stories leaves the expected state")
    void fullSessionAcrossEveryStory() {
        Session session = run(
                "1", "Read chapter 1", "", "",    // US1
                "1", "Write summary", "", "",     // US1
                "1", "Submit assignment", "", "", // US1
                "2",                      // US2
                "3", "2",                 // US3
                "4", "1",                 // US4
                "5",                      // US5
                "2",                      // US2 again
                "6");

        TaskManager manager = session.manager();
        assertEquals(2, manager.getTaskCount());
        assertEquals("Write summary", manager.getTasks().get(0).getName());
        assertTrue(manager.isTaskCompleted(0), "the completed task survived the delete");
        assertEquals("Submit assignment", manager.getTasks().get(1).getName());
        assertFalse(manager.isTaskCompleted(1));

        assertTrue(session.printed("1 of 2 complete."));
        assertTrue(session.printed("status=UP"));
        assertFalse(session.printed("is not a valid choice"),
                "no step of a valid session should be rejected");
        assertTrue(session.printed("Goodbye."));
    }
}
