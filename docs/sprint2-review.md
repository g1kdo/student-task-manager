# Sprint 2 — Review

**Goal:** turn the list into a tracker, and make the running application
observable.
**Committed:** US3 (3), US4 (2), US5 (5) — 10 points.
**Delivered:** US3, US4, US5. 10 of 10 points.

## What was delivered

### US3 — Complete a task

`TaskManager.completeTask(int)` marks the task at a zero-based index complete
and reports whether it changed anything, so completing an already-complete task
is distinguishable from a real change and cannot inflate the completed count.
An index outside the list is refused and logged at `WARN`.

Tests: `TaskManagerTest.CompleteTask` (9), plus menu-level coverage in
`ConsoleAppTest`.

### US4 — Delete a task

`TaskManager.deleteTask(int)` removes the task at an index, renumbers the
remainder implicitly by list order, and keeps the completed count correct when
the deleted task was complete. An index outside the list is refused and logged
at `WARN`.

Tests: `TaskManagerTest.DeleteTask` (9), plus `ConsoleAppTest.deletingRenumbersTheRemainder`.

### US5 — Logs and health check

**Logging.** SLF4J with a Logback backend, configured in
`src/main/resources/logback.xml`. Every record carries a timestamp, a level and
the source logger, and goes to both the console and a rolling daily file under
`logs/` kept for seven days. Levels are used as levels: `INFO` for normal work
(task added, completed, deleted, application starting and shutting down),
`WARN` for a recoverable problem (unknown task number, blank name, unrecognised
menu choice), `ERROR` for a failure (console input failing, health check
reporting `DOWN`).

The first attempt's "logging" was two `System.out.println("[LOG] ...")` lines in
the add and view branches — no framework, no levels, no timestamps, and nothing
at all for complete or delete. All four operations now log, from
`TaskManager` where the state actually changes rather than from the menu.

**Health check.** `HealthCheck.check()` inspects live state and returns a
`HealthStatus`:

| Check | What it does | Verdict it can produce |
|-------|--------------|------------------------|
| `taskStore` | Reads the task count and completed count | `DOWN` if reading throws |
| `heap` | Compares heap in use against the maximum | `DEGRADED` at or above 90% |

It also reports uptime, task count, completed count and heap figures, and never
throws — a health check that fails by throwing cannot report that the
application is unwell. The verdict is logged at a level matching its severity.

The first attempt's health check printed the fixed string
`"Application is running."`, which was true by construction and inspected
nothing.

Tests: `HealthCheckTest` (9), including `DEGRADED`, `DOWN`, the threshold
boundary, and that heap pressure does not soften a `DOWN` verdict.

### Console front end

A six-option menu (`ConsoleApp`), taking its input and output streams as
constructor arguments so the whole loop is testable.

## Demonstration

`docs/evidence/demo-session.txt` records a complete session: two tasks added,
listed, one completed, an out-of-range delete refused, a valid delete, the list
re-read, the health check run, an invalid menu choice reported, then exit.
Reproduce it with:

```bash
./mvnw package -DskipTests
java -jar target/student-task-manager.jar
```

An extract, with the log lines showing levels and timestamps:

```
11:04:33.876 INFO  TaskManager - Task added: 'Read chapter 1' (total now 1)
Task added.

Your tasks:
  1. Read chapter 1                 [Pending]
  2. Write summary                  [Pending]
  0 of 2 complete.

11:04:33.894 WARN  TaskManager - Cannot delete task 9: no such task (there are 1)
Task 9 was not found.

Health: status=UP uptime=0h00m00s tasks=1 completed=1 heap=12.9MB/4006.0MB (0.3%)
  taskStore  readable, holding 1 task(s)
  heap       0.3% used
```

## Improvements from the Sprint 1 retrospective applied in this sprint

| # | Improvement | Status |
|---|-------------|--------|
| 1 | Add a real build file and make CI run the tests | Done — `pom.xml` (`ac292c5`), pipeline (`1aa88a8`) |
| 2 | Never disable a check to go green; prove the pipeline can fail | Done — `docs/evidence/pipeline-fails-on-failing-test.txt` |
| 3 | Test the failure path of every story | Done — 69 tests, 92.3% line coverage (`dcd4276`, `f9aa044`, `1de2787`) |
| 4 | Get printing out of `TaskManager` | Done — `ConsoleApp` owns rendering (`f6a9182`) |
| 5 | Write the review and retrospective as the sprint's work | Done — this document and `sprint2-retrospective.md` |

Improvements 1–4 were carried out in the remediation sprint; see
[remediation-sprint.md](remediation-sprint.md) for the commit-by-commit record
and for what the external review found.

## Sprint 2 goal: met

All three stories work, each acceptance criterion is covered by a named test,
and the application is observable through both a log trail and a health check
that can actually report bad news.
