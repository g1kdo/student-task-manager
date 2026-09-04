# Measured timings

Wall clock. Where a figure is an estimate it says so — the original submission
asserted a "3-4 hour baseline collapsing to minutes" with nothing measured
behind it, and the review was right to mark that down.

| Stage | What was timed | Duration | How measured |
|-------|----------------|----------|--------------|
| 1 | Raw prose request -> Artifact A | not timed | Conversational; not instrumented. Saying so beats inventing a number. |
| 1a | Schema check, 17 ajv cases | < 1 s | `stage1/02-schema-validation.txt` |
| 2 | Copilot Agent writing files | ~2 min 37 s | File mtimes across Copilot's writes, 17:14:10 -> 17:16:47. Excludes prompt-entry and diff-review time, which was not instrumented. |
| 3 | First verification -> commit | ~3 min 25 s | 17:15:35 (session log) -> 17:19:00 (commit 9c2ecf8). Includes diagnosis, the fix and two further verification runs. |
| — | Individual builds | 14 s (fail), 13 s (pass) | `[exit=1, 14s]` / `[exit=0, 13s]`, recorded by `capture.sh` |

Note the overlap: stages 2 and 3 ran concurrently for about 70 seconds, because
the IDE was still writing files when the first verification began. That is a
real property of using the working tree as the hand-off — it carries no
completion signal — and it is recorded rather than tidied away.

## Baseline — an estimate, explicitly

No hand-written control implementation was built, so there is **no measured
baseline**. Reasoning from the artefacts that do exist: Stage 2 produced 329
insertions across 8 files including 14 tests, and the bulk of the manual
equivalent would be writing those boundary tests with the same care over the
inclusive reminder window and the days either side of today. A plausible figure
is **60-90 minutes**, which is reasoned rather than observed and is presented
that way in the report.

To upgrade this to a measurement, the honest route is to implement one
comparable slice by hand — say the `needsReminderOn` boundary tests — time it,
and extrapolate with the extrapolation stated.
