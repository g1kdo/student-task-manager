package app;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the task list and every operation on it.
 *
 * <p>This class holds behaviour only: it never writes to {@code System.out}.
 * Rendering is {@link ConsoleApp}'s job, which keeps the rules here testable
 * without capturing console output and lets the same rules back a different
 * front end later.
 *
 * <p>Every operation reports success through its return value and records what
 * happened through the logger, so a run leaves a readable trail.
 */
public class TaskManager {

    private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

    private final List<Task> tasks = new ArrayList<>();

    /**
     * Adds a task.
     *
     * @return {@code true} if the task was added, {@code false} if the name was
     *         blank and the task was rejected
     */
    public boolean addTask(String name) {
        try {
            Task task = new Task(name);
            tasks.add(task);
            log.info("Task added: '{}' (total now {})", task.getName(), tasks.size());
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("Rejected a task with a blank name: {}", e.getMessage());
            return false;
        }
    }

    public AddResult addTask(String name, LocalDate dueDate, Integer remindBeforeDays) {
        final Task task;
        try {
            task = new Task(name, dueDate, remindBeforeDays);
        } catch (IllegalArgumentException e) {
            log.warn("Rejected task details: {}", e.getMessage());
            return AddResult.REJECTED_INVALID;
        }
        if (hasTaskNamed(task.getName())) {
            log.warn("Rejected duplicate task: '{}'", task.getName());
            return AddResult.REJECTED_DUPLICATE;
        }
        tasks.add(task);
        log.info("Task added: '{}' (total now {})", task.getName(), tasks.size());
        return AddResult.ADDED;
    }

    public boolean hasTaskNamed(String name) {
        if (name == null) {
            return false;
        }
        String normalized = normalizeName(name);
        return tasks.stream().anyMatch(task -> normalizeName(task.getName()).equals(normalized));
    }

    public List<Task> tasksNeedingReminder(LocalDate today) {
        return tasks.stream()
                .filter(task -> task.needsReminderOn(today))
                .sorted(Comparator.comparing(task -> task.getDueDate().orElseThrow()))
                .toList();
    }

    /**
     * @return every task in insertion order, as an unmodifiable view — callers
     *         read the list but cannot bypass the operations on this class to
     *         change it
     */
    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Marks the task at a zero-based index complete.
     *
     * @return {@code true} if the task existed and changed state; {@code false}
     *         if the index was out of range or the task was already complete
     */
    public boolean completeTask(int index) {
        if (!isValidIndex(index)) {
            log.warn("Cannot complete task {}: no such task (there are {})", index + 1, tasks.size());
            return false;
        }
        Task task = tasks.get(index);
        if (!task.complete()) {
            log.debug("Task '{}' was already complete; nothing to do", task.getName());
            return false;
        }
        log.info("Task completed: '{}' ({} of {} now done)",
                task.getName(), getCompletedCount(), tasks.size());
        return true;
    }

    /**
     * Removes the task at a zero-based index.
     *
     * @return {@code true} if a task was removed, {@code false} if the index was
     *         out of range
     */
    public boolean deleteTask(int index) {
        if (!isValidIndex(index)) {
            log.warn("Cannot delete task {}: no such task (there are {})", index + 1, tasks.size());
            return false;
        }
        Task removed = tasks.remove(index);
        log.info("Task deleted: '{}' ({} remaining)", removed.getName(), tasks.size());
        return true;
    }

    public int getTaskCount() {
        return tasks.size();
    }

    /** @return how many tasks are marked complete */
    public int getCompletedCount() {
        return (int) tasks.stream().filter(Task::isCompleted).count();
    }

    /**
     * @throws IndexOutOfBoundsException if {@code index} is out of range
     */
    public boolean isTaskCompleted(int index) {
        return tasks.get(index).isCompleted();
    }

    private boolean isValidIndex(int index) {
        return index >= 0 && index < tasks.size();
    }

    private static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
