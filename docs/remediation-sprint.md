# Remediation Sprint — responding to the assessment review

**Date:** 2026-09-04
**Input:** the assessor's written feedback on the first submission.
**Goal:** close every gap the review identified, and deliver the two backlog
items (US6, US7) that came out of the Sprint 1 retrospective.

The first submission's own record of what went wrong is in
[sprint1-retrospective.md](sprint1-retrospective.md) and
[sprint2-retrospective.md](sprint2-retrospective.md). This document maps each
finding to the change that closed it, so the claim can be checked rather than
taken on trust.

## Traceability

| # | Review finding | What was changed | Commit | Evidence |
|---|----------------|------------------|--------|----------|
| 1 | No `pom.xml` or `build.gradle` anywhere, so `mvn test` could never have worked and the JUnit tests were unrunnable outside IntelliJ | Added a Maven build targeting Java 21, declaring `junit-jupiter`, wiring surefire so a failing test fails the build, plus a script-only Maven wrapper so it runs on a clean machine | `16748eb` | `docs/evidence/test-run.txt` |
| 2 | The workflow's only test step was commented out; a pipeline that cannot fail is not a pipeline | Workflow now compiles, runs the suite, packages the jar, produces coverage and smoke-tests the packaged artefact; triggers cover `develop` as well as `main` | `668df32` | `.github/workflows/ci.yml`, `docs/evidence/pipeline-fails-on-failing-test.txt` |
| 3 | "Logging" was two hardcoded `System.out.println("[LOG] ...")` lines with no framework, levels or timestamps, and nothing for complete or delete | SLF4J + Logback; every record carries a timestamp, level and source logger; all four operations log from `TaskManager`, with `WARN` for recoverable problems and `ERROR` for failures; console plus a rolling daily file | `40f294b` | `docs/evidence/demo-session.txt` |
| 4 | The health check printed a fixed string and inspected nothing | `HealthCheck` reads the task store, measures heap against a 90% threshold, and reports uptime and counts; produces `UP`, `DEGRADED` or `DOWN`; never throws | `353202f` | `HealthCheckTest`, `docs/evidence/demo-session.txt` |
| 5 | Menu options 3 and 4 called `scanner.nextInt()` in a loop whose next read was `scanner.nextLine()`, so "Input a valid digit" printed spuriously after every complete and delete — an untested, user-visible bug | All input read by line through one prompt method; rendering moved out of `TaskManager` into a new `ConsoleApp` that takes its streams as arguments | `40f294b` | Two `REGRESSION`-named tests in `ConsoleAppTest` |
| 6 | Two tests covering only the happy paths of add and complete; nothing exercised `viewTasks`, `deleteTask` or an invalid index | 69 tests: `TaskManagerTest` (30), `TaskTest` (11), `HealthCheckTest` (9), `ConsoleAppTest` (19), covering out-of-range indexes parametrically including `Integer.MIN_VALUE`/`MAX_VALUE`, blank names, repeat completion and end of input | `3fa4f41`, `84fe78b`, `f0e1860` | `docs/evidence/test-run.txt`, `docs/evidence/coverage.txt` |
| 7 | No sprint review and no retrospective anywhere, though the rubric requires two of each | Four documents written, with the timing stated honestly rather than backdated | `ca7300b`, this commit | `sprint1-review.md`, `sprint1-retrospective.md`, `sprint2-review.md`, `sprint2-retrospective.md` |
| 8 | Screenshots and a PDF committed inside `src/app/`, mixing deliverables into a source package | Repository restructured to the Maven standard layout; deliverables moved to `docs/`; the IntelliJ `.iml` module descriptor dropped now that Maven defines the build | `8e8925d` | `find src docs -type f` |
| 9 | The only commit after the single-day burst added an unrelated `StreamArrayExample` | Moved to an `examples` package so the `app` package holds only assessment code, and excluded from the coverage gate as practice code | `8e8925d`, `f0e1860` | `src/main/java/examples/` |
| 10 | Nine of ten commits landed on one day, so both sprints were a single-day exercise | Not retrofittable, and not disguised. The remediation work is a sequence of small commits, each one scoped change with a message stating what and why. The original single-day history is left intact in the log. | — | `git log` |

## Backlog items delivered

### US7 — Automated build and test on every push (3 points)

`./mvnw clean verify` compiles, runs 69 tests, produces a JaCoCo report and
fails below 85% line and 80% branch coverage. `.github/workflows/ci.yml` runs it
on every push and pull request to `main` and `develop`, then packages the jar
and drives it over stdin, asserting on its output — so the pipeline proves the
artefact runs, not merely that it compiled. Test reports, the coverage report
and the jar are published as build artefacts.

That the pipeline can fail is evidenced twice over, and the stronger evidence
was not planned.

**It caught a real defect.** The pipeline's first two runs failed with
`./mvnw: Permission denied` and exit code 126. The Maven wrapper had been
generated on Windows, where git's `core.filemode` is `false`, so git stored
`mvnw` as mode `100644` instead of `100755`; the Linux runner checked out a
file it had no permission to execute. Every local build had been green
throughout, because Git Bash on Windows ignores the permission bit — so this
was only ever observable in CI. It was fixed at the root with
`git update-index --chmod=+x mvnw` rather than by adding a `chmod +x mvnw`
step to the workflow, which would have concealed the cause instead of removing
it — the same mistake as commenting out a failing test step. See
`docs/evidence/ci-runs.txt`.

The same file records the sharpest single piece of evidence against the old
pipeline: `Java CI #2` reports **success** on commit `729b878`, whose entire
purpose was to comment out the test command.

**It also fails on a failing test.** Independently, one assertion was
deliberately inverted, `./mvnw clean verify` was run, `BUILD FAILURE` was
recorded in `docs/evidence/pipeline-fails-on-failing-test.txt`, and the
assertion was restored.

### US6 — Mistyped input is reported rather than crash or be silently misread (3 points)

The stale-newline bug is fixed at the root by reading every line uniformly. Also
covered: a non-numeric menu choice, a numeric choice outside 1–6, a
non-numeric task number, task number 0, a bare Enter, and end of input — which
previously would have spun the loop forever and now exits cleanly. Each has a
named test in `ConsoleAppTest`, and each session runs under a timeout so a
mishandled loop fails the build instead of hanging it.

## Coverage before and after

| | First submission | Now |
|---|---|---|
| Tests | 2 | 69 |
| Runnable outside the IDE | No | Yes (`./mvnw test`) |
| Line coverage | Not measurable | 92.3% |
| Branch coverage | Not measurable | 89.2% |
| Pipeline can fail | No | Yes, demonstrated |
| Operations that log | 2 of 4, no levels | 4 of 4, with levels and timestamps |
| Health check verdicts | 1 (constant) | 3 (`UP`, `DEGRADED`, `DOWN`) |

## What this sprint did not change

- The prototype is still an in-memory console application. Tasks do not survive
  a restart, because persistence was never in the backlog and adding it now
  would be scope the assessment did not ask for.
- The original commit history is unchanged. The single-day burst is visible in
  the log and is discussed in
  [sprint2-retrospective.md](sprint2-retrospective.md) rather than rewritten.
