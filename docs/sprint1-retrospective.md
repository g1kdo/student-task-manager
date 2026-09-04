# Sprint 1 — Retrospective

> **Note on timing.** No retrospective was written during Sprint 1. That
> omission is itself the sprint's clearest process failure, and it is recorded
> here rather than disguised. This document was written on 2026-09-04, after the
> first submission was reviewed. The improvement actions below were carried into
> the remediation work recorded in
> [remediation-sprint.md](remediation-sprint.md).

## What went well

- **Both committed stories were delivered and worked.** `addTask`, `viewTasks`
  and the underlying `Task` model behaved as the acceptance criteria described.
- **Observability was planned, not bolted on.** US5 asked for logs and a health
  check and sat in the backlog from Sprint 0. Most of the failure modes in this
  assessment come from treating monitoring as an afterthought; keeping it as a
  backlog item was the right call.
- **Bounds checks were written deliberately.** `completeTask` and `deleteTask`
  guarded their index from the start, so an out-of-range task number printed a
  message instead of throwing.
- **One real diagnosis was made and recorded.** The `java` package problem was
  understood, not worked around, and the commit message says why the change was
  made.

## What was difficult

- **The build tool was the thing I did not know I needed.** I had JUnit tests
  and a CI workflow and assumed that was a pipeline. It was not: without a
  `pom.xml`, `mvn test` had nothing to run, so the tests were only runnable
  inside my IDE.
- **I treated a red pipeline as a problem with the pipeline.** When `mvn test`
  failed, I commented the step out. That made the symptom go away and left the
  cause — no build file — in place. A pipeline that cannot fail is not a
  pipeline, and I had built exactly that.
- **I tested what I expected to happen.** Two tests, both happy paths. I never
  wrote a test for a task number that does not exist, even though I had written
  the guard for it, so I had no way to know whether the guard still worked.
- **I could not separate the front end from the rules.** `TaskManager` printed
  to `System.out`, so its behaviour could only be checked by looking at the
  console. That is why the menu loop went untested, and why a user-visible bug
  in it shipped.
- **I committed in one sitting.** Nine of ten commits landed on a single day.
  The messages describe real steps, but the history shows one long session, not
  two sprints.

## Improvements for Sprint 2

Five actions, each with the check that proves it happened.

| # | Improvement | How it is verified |
|---|-------------|--------------------|
| 1 | Add a real build file (`pom.xml`) declaring the JUnit dependency, and make CI run `mvn test` instead of commenting it out. | `./mvnw clean verify` runs the suite on a machine with only a JDK; `docs/evidence/test-run.txt` |
| 2 | Never disable a check to make a build green. Fix the cause instead, and prove the pipeline can fail. | `docs/evidence/pipeline-fails-on-failing-test.txt` |
| 3 | Test the failure path of every story, not the happy path alone — starting with the out-of-range task numbers whose guards were already written. | Out-of-range, blank-name and repeat-completion tests in `TaskManagerTest` |
| 4 | Get printing out of `TaskManager` so the rules and the menu can both be tested. | `ConsoleApp` takes its streams as arguments; 19 tests drive the real menu |
| 5 | Write the review and the retrospective as the sprint's work, not after it. Commit them as their own commits. | `docs/sprint2-review.md`, `docs/sprint2-retrospective.md` |

## Backlog changes coming out of this retrospective

Two items were added to the backlog rather than handled informally, so that the
process fixes were tracked like any other work:

- **US7 — automated build and test on every push** (High, 3 points), from
  improvements 1 and 2.
- **US6 — mistyped input is reported rather than crash or be silently
  misread** (High, 3 points), from improvements 3 and 4. Writing tests for the
  menu loop was what surfaced the input bug described in
  [sprint2-retrospective.md](sprint2-retrospective.md).
