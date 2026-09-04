# Sprint 2 — Retrospective

> **Note on timing.** As with Sprint 1, no retrospective was written at the
> time. This document was written on 2026-09-04 alongside the remediation work.

## What went well

- **All three committed stories were delivered**, and US5 in particular went
  from a placeholder to something with real behaviour: logging with levels and
  timestamps across all four operations, and a health check with three possible
  verdicts rather than one.
- **The menu input validation held.** `choice.matches("\\d+")` before
  `Integer.parseInt` was written deliberately and prevented the
  `InputMismatchException` crash that a `Scanner.nextInt()` menu loop normally
  suffers. That instinct was right; it was simply applied in one place and not
  the other.
- **Separating rendering from the rules paid for itself immediately.** Moving
  printing out of `TaskManager` and into `ConsoleApp` was done to make the rules
  testable. It made the *menu* testable too, and that is what caught the bug
  below.
- **Making the health check injectable was worth the extra interface.** Passing
  in a clock and a heap probe meant `DEGRADED` and `DOWN` could be asserted
  directly instead of hoped for. Testing an unhappy path you cannot trigger is
  not testing it.

## What was difficult

- **A user-visible bug shipped, and it shipped precisely where nothing was
  tested.** Menu options 3 and 4 read the task number with
  `scanner.nextInt()`, inside a loop whose next read was `scanner.nextLine()`.
  `nextInt()` consumes the digits but leaves the newline in the buffer, so the
  following `nextLine()` returned an empty string and the menu printed
  "Input a valid digit" after every complete and every delete. The lesson is
  not about `Scanner`: the bug was in the one layer that had no tests, and it
  survived because I checked the code by reading it rather than by running it.
  The fix reads every line uniformly, and two tests named `REGRESSION` in
  `ConsoleAppTest` now pin it.
- **I mistook configuring a thing for having a working thing.** This is the same
  mistake as Sprint 1's commented-out test step, in a different place: two
  `[LOG]` printlns looked like logging, and a fixed status string looked like a
  health check. Both existed and neither did anything. The test I should have
  asked for in both cases is: *what would this tell me if something were
  wrong?*
- **I mixed deliverables into the source tree.** Screenshots and the report PDF
  were committed under `src/app/screenshots/`, so build output and submission
  artefacts lived in the same package as the code.
- **The two sprints were one day.** Nine of ten commits landed on 2026-07-06.
  Whatever the commit messages say, the history does not show two sprints of
  iterative delivery, and I cannot retrofit that.

## Lessons across both sprints

1. **A check that cannot fail is not a check.** It applies to the commented-out
   test step, to `[LOG]` printlns, and to a health check that prints a constant.
   In all three cases something existed where a check was expected, which is
   worse than nothing, because it stops you looking. Every one of them is now
   something that can report bad news, and the pipeline failing on a
   deliberately inverted assertion
   (`docs/evidence/pipeline-fails-on-failing-test.txt`) is the evidence.
2. **Testability is a design property, not a testing activity.** I could not
   test the menu because `ConsoleApp` did not exist and `TaskManager` printed
   to a global stream. The tests became possible once the design let them be
   written — the streams became arguments, the clock became an argument. The
   coverage went from 2 tests to 69 without the rules themselves getting more
   complicated.
3. **Bugs cluster where the tests are not.** The one user-visible defect in the
   prototype was in the only untested layer. That is not coincidence, and it is
   the most useful thing I learned.
4. **When a build goes red, the build is usually telling the truth.** My
   instinct was to silence it. The red step was correctly reporting that there
   was no build file.

## What I would do differently from day one

- Create the `pom.xml` in the first commit, before any code, so the tests are
  runnable from the outside from the very beginning.
- Write the failing test for a story's error path before the code that handles
  it, rather than writing a guard and never checking it.
- Take the streams and the clock as constructor arguments from the start;
  retrofitting it was cheap here only because the project is small.
- Commit the sprint review and retrospective as part of the sprint, on the day,
  so that the reflection is a deliverable rather than a reconstruction.
- Keep `docs/` and `src/` separate from the first commit.
