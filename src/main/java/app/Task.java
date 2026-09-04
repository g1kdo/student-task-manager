package app;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * A single student task: a name, the moment it was created, and whether it has
 * been completed.
 *
 * <p>A task guards its own invariant — a task with a blank name is not a
 * meaningful task, so the constructor rejects one rather than storing it and
 * letting the rest of the application deal with it.
 */
public class Task {

    private final String name;
    private final Instant createdAt;
    private final LocalDate dueDate;
    private final Integer remindBeforeDays;
    private boolean completed;

    /**
     * @param name the task name; must contain at least one non-whitespace character
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public Task(String name) {
        this(name, null, null);
    }

    public Task(String name, LocalDate dueDate, Integer remindBeforeDays) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name must not be blank");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 120) {
            throw new IllegalArgumentException("Task name must be 120 characters or fewer");
        }
        if (remindBeforeDays != null && dueDate == null) {
            throw new IllegalArgumentException("remindBeforeDays requires a dueDate");
        }
        if (remindBeforeDays != null && (remindBeforeDays < 0 || remindBeforeDays > 30)) {
            throw new IllegalArgumentException("remindBeforeDays must be between 0 and 30");
        }
        this.name = trimmed;
        this.createdAt = Instant.now();
        this.dueDate = dueDate;
        this.remindBeforeDays = remindBeforeDays;
        this.completed = false;
    }

    public Optional<LocalDate> getDueDate() {
        return Optional.ofNullable(dueDate);
    }

    public Optional<Integer> getRemindBeforeDays() {
        return Optional.ofNullable(remindBeforeDays);
    }

    public Urgency urgencyOn(LocalDate today) {
        if (dueDate == null) {
            return Urgency.NONE;
        }
        if (dueDate.isBefore(today)) {
            return Urgency.OVERDUE;
        }
        if (dueDate.isEqual(today)) {
            return Urgency.DUE_TODAY;
        }
        return Urgency.UPCOMING;
    }

    public boolean needsReminderOn(LocalDate today) {
        if (completed || dueDate == null || remindBeforeDays == null) {
            return false;
        }
        return !today.isBefore(dueDate.minusDays(remindBeforeDays)) && !today.isAfter(dueDate);
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    /**
     * Marks this task complete.
     *
     * @return {@code true} if this call changed the task, {@code false} if it
     *         was already complete. Callers use the result to distinguish a
     *         real state change from a no-op when reporting or logging.
     */
    public boolean complete() {
        if (completed) {
            return false;
        }
        completed = true;
        return true;
    }

    @Override
    public String toString() {
        return name + " - " + (completed ? "Done" : "Pending");
    }
}
