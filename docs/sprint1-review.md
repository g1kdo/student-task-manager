# Sprint 1 — Review

**Goal:** a student can record a task and read the list back.
**Committed:** US1 (3 points), US2 (2 points) — 5 points, plus repository and
pipeline setup.
**Delivered:** US1, US2. 5 of 5 points.

## What was delivered

### US1 — Add a task

`TaskManager.addTask(String)` stores a `Task` and confirms it. `Task` is a small
model holding a name and a completion flag.

Commits: `a95258a` initial project setup, `27fdf4c` moving `Task` into the `app`
package, `8aeb39e` implementing `TaskManager`.

### US2 — View tasks

Tasks are listed in insertion order, numbered from 1, each showing whether it is
done or pending.

Commit: `8aeb39e`.

### Repository and pipeline setup

- Git repository with a `main` and a `develop` branch, and a pull request
  template (`.github/PULL_REQUEST_TEMPLATE.md`).
- A GitHub Actions workflow checking out the code and installing Temurin 21
  (`629bd17`).
- Two JUnit 5 tests for `addTask` and `completeTask` (`88e15bb`).

## Demonstration

Run at the time from IntelliJ. The archived screenshot
`docs/archive/first-attempt/tests-two-happy-path.png` shows both tests passing,
and `docs/archive/first-attempt/cicd1-no-test-step.png` shows the pipeline run.

## A real problem diagnosed

`Task` was first placed in a package named `java`. The JVM refuses to load user
classes from a package under `java.`, so the class would not run. The package
was renamed to `app` — recorded in commit `27fdf4c`, whose message states the
diagnosis rather than just the change.

## Sprint 1 goal: met with a significant caveat

Both stories work, and the acceptance criteria as written in Sprint 0 are
satisfied. But the *pipeline* deliverable was not really met, and this was not
recognised at the time:

- The workflow's only test step was committed as a comment
  (`729b878`, "Comment out Maven test command in workflow"). The pipeline could
  not fail, so it verified nothing.
- The reason the step was commented out was that `mvn test` did not work — and
  the reason for *that* was that the repository had no `pom.xml` or
  `build.gradle` at all. The tests only ran inside one IntelliJ project.
- Commenting out the check treated the symptom. The cause was a missing build
  file.

This is carried into the retrospective as the sprint's main finding.
