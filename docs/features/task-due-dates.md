# Feature spec — task due dates, duplicate rejection and derived urgency

Artifact A. Produced by the Stage 1 chat UX from the raw feature request in
`docs/lab3/artefacts/stage1/00-raw-feature-request.md`. Design only: anything
that must bind to real code is left as a `TODO` for Stage 2 to close.

## 1. Creation contract (JSON Schema, draft 2020-12)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "TaskCreate",
  "type": "object",
  "properties": {
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 120,
      "description": "Trimmed before storage. Must contain a non-whitespace character."
    },
    "dueDate": {
      "type": ["string", "null"],
      "format": "date",
      "description": "ISO-8601 local date, e.g. 2026-09-30. Null or absent means no due date."
    },
    "remindBeforeDays": {
      "type": ["integer", "null"],
      "minimum": 0,
      "maximum": 30,
      "description": "Days before dueDate to surface a reminder. Meaningful only when dueDate is set."
    }
  },
  "required": ["name"],
  "additionalProperties": false,
  "allOf": [
    {
      "if": {
        "properties": { "remindBeforeDays": { "type": "integer" } },
        "required": ["remindBeforeDays"]
      },
      "then": {
        "properties": { "dueDate": { "type": "string" } },
        "required": ["dueDate"]
      }
    }
  ]
}
```

The `if`/`then` is the load-bearing part of this contract. It states the one
rule the prose only implied — *a reminder makes no sense without a due date* —
in a form a machine can check, so Stage 2 has no room to interpret it as
optional. Everything else in the schema is a constraint a reviewer can read off
in one pass: name non-blank and bounded, due date a real date or absent,
reminder lead time a bounded non-negative integer.

## 2. Model changes

```java
// ---- Task: two new fields, both optional, invariants guarded here ----------

public class Task {

    private final String name;
    private final Instant createdAt;
    private final LocalDate dueDate;            // nullable: no due date
    private final Integer remindBeforeDays;     // nullable: no reminder
    private boolean completed;

    // ASSUMPTION: the existing single-argument constructor stays, delegating
    // with both new fields null, so all 69 existing tests keep compiling.
    public Task(String name) {
        this(name, null, null);
    }

    public Task(String name, LocalDate dueDate, Integer remindBeforeDays) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Task name must not be blank");
        }
        // ASSUMPTION: 120 chars from the schema; a console list is unreadable beyond that.
        String trimmed = name.trim();
        if (trimmed.length() > 120) {
            throw new IllegalArgumentException("Task name must be 120 characters or fewer");
        }
        // The if/then rule from section 1, enforced where it cannot be bypassed.
        if (remindBeforeDays != null && dueDate == null) {
            throw new IllegalArgumentException("remindBeforeDays requires a dueDate");
        }
        if (remindBeforeDays != null && (remindBeforeDays < 0 || remindBeforeDays > 30)) {
            throw new IllegalArgumentException("remindBeforeDays must be between 0 and 30");
        }
        this.name = trimmed;
        this.dueDate = dueDate;
        this.remindBeforeDays = remindBeforeDays;
        this.createdAt = Instant.now();
        this.completed = false;
    }

    public Optional<LocalDate> getDueDate() { return Optional.ofNullable(dueDate); }
    public Optional<Integer> getRemindBeforeDays() { return Optional.ofNullable(remindBeforeDays); }

    // ASSUMPTION: urgency is derived, never stored — storing it would go stale
    // the moment the clock passes midnight.
    public Urgency urgencyOn(LocalDate today) {
        if (dueDate == null) return Urgency.NONE;
        if (dueDate.isBefore(today)) return Urgency.OVERDUE;
        if (dueDate.isEqual(today)) return Urgency.DUE_TODAY;
        return Urgency.UPCOMING;
    }

    // ASSUMPTION: a completed task is never chased, whatever its due date.
    public boolean needsReminderOn(LocalDate today) {
        if (completed || dueDate == null || remindBeforeDays == null) return false;
        return !today.isBefore(dueDate.minusDays(remindBeforeDays)) && !today.isAfter(dueDate);
    }
}
```

Note that a past due date is *accepted*, not rejected. A student entering a
task they have already missed is reporting reality, and the right response is
to show it as `OVERDUE`, not to refuse the input. This is an assumption worth
challenging at the approval gate.

## 3. API surface

```java
// ---- Derived urgency -------------------------------------------------------

public enum Urgency { OVERDUE, DUE_TODAY, UPCOMING, NONE }

// ---- Outcome type: lets the caller tell a duplicate from a success ---------

// ASSUMPTION: the existing boolean returns cannot express three outcomes, so
// add an enum rather than overload null/false with two meanings.
public enum AddResult { ADDED, REJECTED_DUPLICATE, REJECTED_INVALID }

// ---- TaskManager ----------------------------------------------------------

/** Existing signature, kept: delegates with no due date and no reminder. */
public boolean addTask(String name);

/** @return ADDED, REJECTED_DUPLICATE if a task with this name already exists,
 *          or REJECTED_INVALID if the contract in section 1 is violated. */
public AddResult addTask(String name, LocalDate dueDate, Integer remindBeforeDays);

/** Duplicate detection. ASSUMPTION: case- and whitespace-insensitive —
 *  "Revise maths" and "revise maths " are the same task to a human. */
public boolean hasTaskNamed(String name);

/** Tasks needing a reminder on the given date, in due-date order. */
public List<Task> tasksNeedingReminder(LocalDate today);

// TODO(stage-2): bind to the real TaskManager — it already holds
//   List<Task> tasks, getTasks(), getTaskCount(), getCompletedCount(),
//   completeTask(int), deleteTask(int) and an SLF4J logger. Reuse all of them.
// TODO(stage-2): log a rejected duplicate at WARN with the offending name,
//   matching the existing log style in TaskManager.
// TODO(stage-2): ConsoleApp must render urgency in the task list and prompt for
//   the two new optional fields on add, accepting empty input as "not set".
//   Reuse the existing readLine-based prompt; do not introduce a new Scanner.
// TODO(stage-2): clock injection — urgencyOn/needsReminderOn take a LocalDate
//   so tests are deterministic. Decide where ConsoleApp gets "today" from and
//   make it overridable in tests.
```

## 4. Test outline

```markdown
Contract validation (section 1)
- name blank / whitespace only / null -> REJECTED_INVALID, nothing stored
- name longer than 120 chars -> REJECTED_INVALID
- name is trimmed before storage
- remindBeforeDays set with no dueDate -> REJECTED_INVALID  (the if/then rule)
- remindBeforeDays negative, and 31 -> REJECTED_INVALID (boundary: 0 and 30 accepted)

Duplicate rejection (the 409 analogue)
- adding the same name twice -> second returns REJECTED_DUPLICATE, count stays 1
- duplicate check ignores case and surrounding whitespace
- a name freed by deleteTask can be added again
- a duplicate is rejected even when the due dates differ

Derived urgency — boundaries either side of today, not just the happy path
- dueDate == today            -> DUE_TODAY
- dueDate == today.minusDays(1) -> OVERDUE
- dueDate == today.plusDays(1)  -> UPCOMING
- no dueDate                  -> NONE
- urgency is recomputed against the date passed in, never cached

Reminders
- today exactly on dueDate.minusDays(remindBeforeDays) -> reminder due (inclusive lower bound)
- one day earlier than that                            -> no reminder
- today == dueDate                                     -> reminder still due (inclusive upper bound)
- today after dueDate                                  -> no reminder
- remindBeforeDays == 0                                -> reminder only on the due date itself
- a completed task never needs a reminder, whatever its dates

Regression
- all 69 existing tests still pass, unchanged
- the single-argument addTask still behaves exactly as before
```
