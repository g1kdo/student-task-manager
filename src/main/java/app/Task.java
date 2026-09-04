package app;

import java.time.Instant;

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
    private boolean completed;

    /**
     * @param name the task name; must contain at least one non-whitespace character
     * @throws IllegalArgumentException if {@code name} is null or blank
     */
    public Task(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name must not be blank");
        }
        this.name = name.trim();
        this.createdAt = Instant.now();
        this.completed = false;
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
