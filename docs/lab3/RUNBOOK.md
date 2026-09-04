# Lab 3 runbook — executing the three stages

The point of this lab redo is that the workflow is **run**, not designed. This
file is the operating procedure, and each step says which artefact it must
leave behind. A step with no artefact did not happen.

Branch: `feature/task-due-dates`. The graded Agile & DevOps submission stays on
`develop` untouched.

---

## Stage 1 — Chat UX ✅ DONE

Produced the design artefact from the raw prose request, with no repository
edits.

| Artefact | File |
|---|---|
| Raw feature request (the single input) | `artefacts/stage1/00-raw-feature-request.md` |
| Exact prompt issued | `artefacts/stage1/01-prompt.txt` |
| **Artifact A** — the feature spec | `../features/task-due-dates.md` |
| Executed check: schema compiles and its `if`/`then` rule holds | `artefacts/stage1/02-schema-validation.txt` |

Reproduce the check:

```bash
cd docs/lab3/artefacts/stage1
npm install
npm run check
```

---

## Stage 2 — IDE UX ⬜ YOURS TO RUN

**Tool:** VS Code + GitHub Copilot, Agent mode.

1. Open the repository in VS Code and make sure you are on the branch:

```bash
git checkout feature/task-due-dates
```

2. Capture the starting point, so the diff is provable later:

```bash
git rev-parse HEAD > docs/lab3/artefacts/stage2/00-base-commit.txt
```

3. Open Copilot Chat, switch to **Agent** mode, and paste the prompt in
   `artefacts/stage2/01-prompt.txt` verbatim.

4. **Review the diff it offers before accepting.** This is the stage's human
   approval gate. If it invents a class, duplicates something that exists, or
   touches the pom or the workflow, reject and say so — that rejection is worth
   recording.

5. Once you have accepted the changes, capture what actually happened:

```bash
# from the repository root
git diff > docs/lab3/artefacts/stage2/02-working-tree.diff
git status --short > docs/lab3/artefacts/stage2/03-status.txt
```

6. Note two things in `artefacts/stage2/04-notes.md` — a new file, plain prose:
   - roughly how long the stage took, wall clock;
   - **any friction**: a model that refused, a suggestion you rejected, a
     retry, anything that did not go to plan. Do not smooth these over. Tool
     friction absorbed without redesigning the workflow is the strongest
     available evidence for the Adaptability criterion, and it is precisely
     what the passing submission in this cohort did well.

7. Screenshot the Copilot Chat panel showing the diff, and save it as
   `artefacts/stage2/05-copilot-diff.png`.

**Do not run the build yourself.** Leaving the tree unverified is what gives
Stage 3 something real to do — and if Copilot's code is broken, Stage 3
catching that is a genuine result worth reporting, not a problem.

---

## Stage 3 — CLI UX ⬜ RUN AFTER STAGE 2

**Tool:** Claude Code (this CLI agent).

Tell me Stage 2 is done and I will execute it: run `./mvnw clean verify`,
self-heal within a two-attempt limit, re-run the full suite, stage the changes,
author a Conventional Commit message from `git diff --staged` rather than from
anyone's description, and print the SHA. Every command and its real output gets
captured to `artefacts/stage3/`.

---

## Timing

Fill in `artefacts/timings.md` as you go. The original submission claimed a
"3–4 hour baseline collapsing to minutes" with nothing measured behind it; the
reviewer marked that down. Wall-clock figures per stage, even rough ones, are
worth more than an estimate.
