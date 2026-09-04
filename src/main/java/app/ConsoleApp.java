package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The console front end: draws the menu, reads the user's input and renders
 * what {@link TaskManager} reports back.
 *
 * <p>The streams are constructor arguments rather than {@code System.in} and
 * {@code System.out}, so a test can drive the whole menu from a
 * {@code ByteArrayInputStream} and assert on the captured output.
 *
 * <p>All input is read a whole line at a time through {@link #prompt}. The
 * previous version mixed {@code Scanner.nextInt()} for task numbers with
 * {@code Scanner.nextLine()} for the menu choice; {@code nextInt()} leaves the
 * newline in the buffer, so the next {@code nextLine()} returned an empty
 * string and the menu rejected it. Reading lines uniformly removes that class
 * of bug instead of patching around it.
 */
public class ConsoleApp {

    private static final Logger log = LoggerFactory.getLogger(ConsoleApp.class);

    private final BufferedReader in;
    private final PrintStream out;
    private final TaskManager manager;

    public ConsoleApp(InputStream in, PrintStream out, TaskManager manager) {
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.manager = manager;
    }

    public ConsoleApp(InputStream in, PrintStream out) {
        this(in, out, new TaskManager());
    }

    /** Runs the menu loop until the user exits or the input stream ends. */
    public void run() {
        log.info("Student Task Manager starting up");
        try {
            boolean running = true;
            while (running) {
                printMenu();
                String choice = prompt("Enter your choice: ");
                if (choice == null) {
                    out.println();
                    out.println("Input ended. Exiting.");
                    log.info("Input stream closed; shutting down");
                    return;
                }
                running = handle(choice.trim());
            }
        } catch (IOException e) {
            // Reading the console failed. There is nothing useful to retry, so
            // report it plainly and record it for whoever reads the log.
            out.println("Could not read from the console: " + e.getMessage());
            log.error("Console input failed; shutting down", e);
        }
        log.info("Student Task Manager shut down");
    }

    /** @return {@code false} when the menu loop should stop */
    private boolean handle(String choice) throws IOException {
        switch (choice) {
            case "1":
                return addTask();
            case "2":
                viewTasks();
                return true;
            case "3":
                return completeTask();
            case "4":
                return deleteTask();
            case "5":
                healthCheck();
                return true;
            case "6":
                out.println("Goodbye.");
                return false;
            case "":
                // A bare Enter is not a mistake worth complaining about; redraw.
                return true;
            default:
                out.println(quote(choice) + " is not a valid choice. Enter a number from 1 to 6.");
                log.warn("Unrecognised menu choice: {}", quote(choice));
                return true;
        }
    }

    private boolean addTask() throws IOException {
        String name = prompt("Task name: ");
        if (name == null) {
            return false;
        }
        if (manager.addTask(name)) {
            out.println("Task added.");
        } else {
            out.println("A task needs a name. Nothing was added.");
        }
        return true;
    }

    private void viewTasks() {
        List<Task> tasks = manager.getTasks();
        if (tasks.isEmpty()) {
            out.println("You have no tasks yet.");
            return;
        }
        out.println();
        out.println("Your tasks:");
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            out.printf("  %d. %-30s %s%n",
                    i + 1, task.getName(), task.isCompleted() ? "[Done]" : "[Pending]");
        }
        out.printf("  %d of %d complete.%n", manager.getCompletedCount(), tasks.size());
    }

    private boolean completeTask() throws IOException {
        if (manager.getTaskCount() == 0) {
            out.println("You have no tasks yet.");
            return true;
        }
        String raw = prompt("Task number: ");
        if (raw == null) {
            return false;
        }
        Integer number = parseTaskNumber(raw);
        if (number == null) {
            return true;
        }
        if (manager.completeTask(number - 1)) {
            out.println("Task completed.");
        } else {
            out.println("Task " + number + " was not found, or it is already complete.");
        }
        return true;
    }

    private boolean deleteTask() throws IOException {
        if (manager.getTaskCount() == 0) {
            out.println("You have no tasks yet.");
            return true;
        }
        String raw = prompt("Task number: ");
        if (raw == null) {
            return false;
        }
        Integer number = parseTaskNumber(raw);
        if (number == null) {
            return true;
        }
        if (manager.deleteTask(number - 1)) {
            out.println("Task deleted.");
        } else {
            out.println("Task " + number + " was not found.");
        }
        return true;
    }

    private void healthCheck() {
        // Still a fixed string. Replaced with a real state inspection next.
        out.println("Application is running.");
    }

    /**
     * @return the task number the user typed, or {@code null} if what they
     *         typed was not a whole number
     */
    private Integer parseTaskNumber(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.matches("\\d+")) {
            out.println(quote(trimmed) + " is not a task number.");
            log.warn("Rejected a task number that was not a whole number: {}", quote(trimmed));
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            // Reachable for a run of digits too long to fit in an int.
            out.println(quote(trimmed) + " is not a task number.");
            log.warn("Rejected an out-of-range task number: {}", quote(trimmed));
            return null;
        }
    }

    /** @return the line the user typed, or {@code null} at end of input */
    private String prompt(String label) throws IOException {
        out.print(label);
        out.flush();
        return in.readLine();
    }

    private void printMenu() {
        out.println();
        out.println("Task Manager");
        out.println("1. Add Task");
        out.println("2. View Tasks");
        out.println("3. Complete Task");
        out.println("4. Delete Task");
        out.println("5. Health Check");
        out.println("6. Exit");
        out.println();
    }

    private static String quote(String value) {
        return "'" + value + "'";
    }
}
