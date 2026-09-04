# Sprint 0 — Planning

## Product vision

A small Java console application that lets a student capture the tasks they have
to do, see what is still outstanding, and tick things off — so that nothing is
tracked only in their head.

## Product backlog

Estimated in story points on a relative scale (1 = trivial, 2 = small,
3 = a morning's work, 5 = needs design as well as code).

| ID | User story | Priority | Points | Sprint |
|----|------------|----------|--------|--------|
| US1 | As a student, I want to add a task, so that I can stop keeping it in my head. | High | 3 | 1 |
| US2 | As a student, I want to see all my tasks with their status, so that I know what is outstanding. | High | 2 | 1 |
| US3 | As a student, I want to mark a task complete, so that I can track my progress. | High | 3 | 2 |
| US4 | As a student, I want to delete a task, so that my list stays relevant. | Medium | 2 | 2 |
| US5 | As a developer, I want application logs and a health check, so that I can see how the running system is behaving. | Medium | 5 | 2 |
| US6 | As a student, I want mistyped input to be reported rather than crash or be silently misread, so that I can correct it and carry on. | High | 3 | Added at grooming |
| US7 | As a developer, I want the tests to run automatically on every push, so that a regression is caught before review. | High | 3 | Added at grooming |

**Total: 21 points.** US1–US5 were the original Sprint 0 backlog (13 points).
US6 and US7 were added during backlog grooming after the Sprint 1 review, which
is recorded in [sprint1-retrospective.md](sprint1-retrospective.md).

## Acceptance criteria

Written so each line can be checked by a test rather than by opinion. The test
that covers each one is named in brackets.

### US1 — Add a task

- Given a non-empty name, the task is stored and the application confirms it
  (`ConsoleAppTest.addingATaskConfirmsAndStores`)
- Tasks appear in the order they were added
  (`TaskManagerTest.AddTask.addPreservesInsertionOrder`)
- A blank or whitespace-only name is refused and nothing is stored
  (`TaskManagerTest.AddTask.addRejectsBlankNames`, `ConsoleAppTest.emptyTaskNameIsRefused`)
- Surrounding whitespace is trimmed from the stored name
  (`TaskManagerTest.AddTask.addTrimsTheName`)

### US2 — View tasks

- Every task is listed, numbered from 1
  (`ConsoleAppTest.listShowsNumbersAndStatuses`)
- Each task shows `[Done]` or `[Pending]`
  (`ConsoleAppTest.listShowsNumbersAndStatuses`)
- A summary line reports how many of how many are complete
  (`ConsoleAppTest.listShowsNumbersAndStatuses`)
- With no tasks, the application says so rather than printing an empty list or erroring
  (`ConsoleAppTest.viewingAnEmptyListSaysSo`)

### US3 — Complete a task

- Given a valid task number, the task becomes complete and the application confirms it
  (`ConsoleAppTest`, `TaskManagerTest.CompleteTask.completeMarksTheTaskDone`)
- Completing an already-complete task is reported and does not change the completed count
  (`TaskManagerTest.CompleteTask.completeIsIdempotent`, `ConsoleAppTest.completingTwiceIsReported`)
- A task number that does not exist — including 0 and a negative number — is
  reported and leaves every task untouched
  (`TaskManagerTest.CompleteTask.completeRejectsOutOfRangeIndexes`, `ConsoleAppTest.taskNumberZeroIsReported`)
- With no tasks, the application says so and does not ask for a number
  (`ConsoleAppTest.completingWithNoTasksSaysSo`)

### US4 — Delete a task

- Given a valid task number, the task is removed and the application confirms it
  (`TaskManagerTest.DeleteTask.deleteRemovesTheTask`)
- The remaining tasks are renumbered without gaps
  (`ConsoleAppTest.deletingRenumbersTheRemainder`)
- Deleting a completed task lowers the completed count
  (`TaskManagerTest.DeleteTask.deleteUpdatesTheCompletedCount`)
- A task number that does not exist is reported and nothing is removed
  (`TaskManagerTest.DeleteTask.deleteRejectsOutOfRangeIndexes`)

### US5 — Logs and health check

- Every add, complete and delete produces a log record carrying a timestamp, a
  level and the source logger (`docs/evidence/demo-session.txt`)
- An operation that fails — an unknown task number, a blank name, an
  unrecognised menu choice — logs at `WARN`, not `INFO`
  (`docs/evidence/demo-session.txt`)
- The health check reports live state: uptime, task count, completed count and
  heap use, not a fixed string
  (`HealthCheckTest.healthyApplicationReportsUp`, `ConsoleAppTest.healthCheckReportsLiveState`)
- The health check reports `DEGRADED` under heap pressure and `DOWN` when the
  task store cannot be read, and never throws
  (`HealthCheckTest.heapPressureReportsDegraded`, `HealthCheckTest.unreadableTaskStoreReportsDown`)

### US6 — Mistyped input

- A menu choice that is not one of the six options is reported by name and the
  loop continues (`ConsoleAppTest.nonNumericChoiceIsReported`, `ConsoleAppTest.outOfRangeChoiceIsReported`)
- A task number that is not a whole number is reported and nothing changes
  (`ConsoleAppTest.nonNumericTaskNumberIsReported`)
- Pressing Enter on its own redraws the menu without reporting an error
  (`ConsoleAppTest.blankChoiceIsIgnored`)
- Entering a task number never causes the *next* menu read to be misread
  (`ConsoleAppTest.completingATaskDoesNotBreakTheNextMenuRead`, `.deletingATaskDoesNotBreakTheNextMenuRead`)
- Reaching the end of input exits cleanly instead of looping forever
  (`ConsoleAppTest.endOfInputExitsCleanly`)

### US7 — Automated build and test

- `./mvnw clean verify` compiles, runs every test and reports coverage on a
  machine with only a JDK installed (`docs/evidence/test-run.txt`)
- The pipeline runs on every push and pull request to `main` and `develop`
  (`.github/workflows/ci.yml`)
- A failing test fails the pipeline
  (`docs/evidence/pipeline-fails-on-failing-test.txt`)
- The pipeline runs the packaged artefact and asserts on its output, not only
  that it compiled (the smoke-test step in `.github/workflows/ci.yml`)
- Test reports, the coverage report and the runnable jar are published as build
  artefacts (`.github/workflows/ci.yml`)

## Definition of Done

A backlog item is done when all of the following hold. The first version of this
list ("code compiles, feature works, unit test passes, code committed, no major
bugs") was too loose to fail anything — the revised list can.

1. `./mvnw clean verify` passes locally: compiles, all tests green, coverage
   gate met.
2. Every acceptance criterion for the item is covered by at least one named
   automated test.
3. Both the success and the failure path of the item are tested, not the happy
   path alone.
4. The operation logs at a level matching what happened — `INFO` for normal
   work, `WARN` or `ERROR` for a problem.
5. The work is committed in increments, each message saying what changed and
   why.
6. The CI pipeline is green on the pushed commit.
7. `README.md` and the relevant sprint document reflect any change in
   behaviour.
8. No deliverables (screenshots, reports) are committed inside the source tree.

## Sprint 1 plan

**Selected:** US1 (3), US2 (2) — 5 points, plus repository and CI setup.

Chosen because the list is useless until something can be put on it and read
back, and because everything later depends on the storage model these two
stories establish.

## Sprint 2 plan

**Selected:** US3 (3), US4 (2), US5 (5) — 10 points.

Completing and deleting are what turn a list into a tracker, and US5 was
deliberately kept in the backlog from the start rather than bolted on, so that
observability was planned work rather than an afterthought.

US6 (3) and US7 (3) were added at grooming after the Sprint 1 review and
delivered in the remediation sprint — see
[remediation-sprint.md](remediation-sprint.md).
