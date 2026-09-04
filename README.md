# Student Task Manager

A Java console application for managing student tasks, built as the individual
final assessment for **Agile & DevOps in Practice**.

[![CI](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)

The product is deliberately small. What the assessment is about is how it was
planned, delivered, verified and reflected on — those deliverables are in
[`docs/`](docs/).

## Features

| Story | Feature |
|-------|---------|
| US1 | Add a task, with a blank name refused rather than stored |
| US2 | List all tasks, numbered, each showing `[Done]` or `[Pending]`, with a completed count |
| US3 | Mark a task complete; completing one twice is reported and does not double-count |
| US4 | Delete a task; the rest renumber and the completed count stays correct |
| US5 | Timestamped, levelled logging to console and a rolling file, plus a health check that inspects live state |
| US6 | Mistyped menu choices and task numbers are reported; the loop continues |
| US7 | Every push runs the full suite, the coverage gate and a smoke test of the packaged jar |

## Requirements

JDK 21. Maven is **not** required — the repository ships a wrapper.

## Build and run

```bash
./mvnw clean verify
```

Compiles, runs all 69 tests, writes a JaCoCo report to
`target/site/jacoco/index.html`, and fails if line coverage drops below 85% or
branch coverage below 80%.

```bash
./mvnw package -DskipTests
java -jar target/student-task-manager.jar
```

On Windows use `mvnw.cmd` in place of `./mvnw`.

Running the tests alone:

```bash
./mvnw test
```

## Using it

```
Task Manager
1. Add Task
2. View Tasks
3. Complete Task
4. Delete Task
5. Health Check
6. Exit
```

Option 5 reports live state rather than a fixed message:

```
Health: status=UP uptime=0h04m12s tasks=3 completed=1 heap=14.2MB/4006.0MB (0.4%)
  taskStore  readable, holding 3 task(s)
  heap       0.4% used
```

`status` is `UP`, `DEGRADED` (heap at or above 90% of the maximum) or `DOWN`
(the task store could not be read).

## Logging

SLF4J with a Logback backend, configured in
[`src/main/resources/logback.xml`](src/main/resources/logback.xml). Records go to
the console and to a daily rolling file under `logs/`, kept for seven days.
Levels carry meaning: `INFO` for normal operations, `WARN` for a recoverable
problem such as an unknown task number, `ERROR` for a failure such as the health
check reporting `DOWN`.

## Layout

```
pom.xml                        Maven build: Java 21, JUnit 5, surefire, JaCoCo, jar
mvnw, mvnw.cmd, .mvn/          Maven wrapper — no local Maven install needed
.github/workflows/ci.yml       Build, test, coverage gate, package, smoke test
src/main/java/app/             Task, TaskManager, HealthCheck, HealthStatus, ConsoleApp, Main
src/main/java/examples/        Standalone practice classes, not part of the prototype
src/main/resources/            logback.xml
src/test/java/app/             69 tests
docs/                          Assessment deliverables (see below)
```

## Assessment deliverables

| Deliverable | Where |
|-------------|-------|
| Backlog, estimates, acceptance criteria, Definition of Done, sprint plans | [`docs/sprint0-planning.md`](docs/sprint0-planning.md) |
| Sprint 1 review | [`docs/sprint1-review.md`](docs/sprint1-review.md) |
| Sprint 1 retrospective | [`docs/sprint1-retrospective.md`](docs/sprint1-retrospective.md) |
| Sprint 2 review | [`docs/sprint2-review.md`](docs/sprint2-review.md) |
| Sprint 2 retrospective | [`docs/sprint2-retrospective.md`](docs/sprint2-retrospective.md) |
| Response to the assessment review, finding by finding | [`docs/remediation-sprint.md`](docs/remediation-sprint.md) |
| CI/CD, testing and demo evidence | [`docs/evidence/`](docs/evidence/) |
| Consolidated report (LaTeX source and PDF) | [`docs/report/`](docs/report/) |
| First attempt, kept for comparison | [`docs/archive/`](docs/archive/) |

## Architecture

Four small classes with one job each, which is what makes the tests possible:

- **`Task`** — the model. Guards its own invariant: a blank name is refused at
  construction.
- **`TaskManager`** — the rules. Owns the list, reports success through return
  values and logs what happened. Never writes to `System.out`.
- **`HealthCheck` / `HealthStatus`** — observability. Takes a clock and a heap
  probe as arguments so its unhappy paths can be tested deterministically.
- **`ConsoleApp`** — the front end. Owns all rendering and takes its input and
  output streams as arguments, so tests drive the real menu loop with scripted
  keystrokes.
- **`Main`** — the wiring that supplies the real console.
