# Measured timings

Wall clock, filled in as each stage ran. The original submission asserted a
"3-4 hour baseline collapsing to minutes" with nothing measured; these are the
real figures, and where a figure is an estimate it says so.

| Stage | What was timed | Duration | How measured |
|-------|----------------|----------|--------------|
| 1 | Raw prose request -> Artifact A, plus the schema check | | |
| 2 | Copilot Agent prompt -> accepted diff in the working tree | | wall clock, noted in stage2/04-notes.md |
| 3 | `mvnw clean verify` -> green suite -> commit authored and SHA printed | | `date` before and after, in stage3 log |
| — | **Total workflow** | | |

## Baseline for comparison

The baseline must be measured or clearly labelled an estimate. Options, in
descending order of honesty:

1. **Measured**: implement an equivalent feature by hand and time it. Best
   evidence, costs an afternoon.
2. **Partially measured**: time one comparable slice — e.g. hand-writing the
   boundary tests for `urgencyOn` — and extrapolate, saying so explicitly.
3. **Labelled estimate**: state a figure and label it an estimate with the
   reasoning shown.

Option 2 is the sensible target here. Whatever is used, the report must not
present it as a measurement if it is not one — that specific overclaim is what
the review criticised.
