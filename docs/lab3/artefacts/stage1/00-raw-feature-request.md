# Stage 1 input — the raw feature request

This is the single unstructured input that flows end-to-end through all three
stages. It is deliberately written the way a product owner would file it: prose,
with the edge cases implied rather than specified.

> Let students give a task a due date so they can tell what's urgent. The due
> date is optional. If they want reminding, we need to know how many days ahead
> — but a reminder makes no sense on a task with no due date. Don't let them add
> the same task twice; tell them it already exists instead of silently adding a
> duplicate. And when they list tasks, show which ones are overdue, which are
> due today, and which are still upcoming.

## Why this feature was chosen

Not a toy. It exercises the four things that make a feature worth putting
through a multi-stage workflow rather than typing by hand:

1. **A data contract with a conditional rule** — `remindBeforeDays` is only
   meaningful when `dueDate` is present. Conditional requirement is exactly the
   kind of rule that is easy to state in prose and easy to get wrong in code.
2. **A conflict rule** — rejecting a duplicate task instead of adding it. The
   console analogue of returning HTTP 409.
3. **Derived state with real branching** — `OVERDUE` / `DUE_TODAY` /
   `UPCOMING` / `NONE` is computed, not stored, and has boundary conditions
   either side of "today" that a return-value-only test can miss.
4. **It lands in an existing, already-tested codebase** — 69 passing tests and
   an enforced coverage gate. The workflow has to produce a genuinely additive
   result that keeps the suite green, not a plausible-looking result on a
   throwaway project.
