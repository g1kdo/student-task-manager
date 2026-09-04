# Stage 2 notes — GitHub Copilot, Agent mode, VS Code

## Timing (measured)

| Event | Time |
|---|---|
| Prompt submitted to Copilot Chat | 17:10 |
| Copilot finished | 17:18 |
| **Elapsed** | **~8 minutes** |

Diff review was done by following along as Copilot worked, rather than as a
separate pass at the end.

For comparison, file modification times show Copilot's actual writes landing
between 17:14:10 and 17:16:47 — about 2 min 37 s of file output inside the
8-minute stage. The remainder is the agent reading the five referenced files,
reasoning, and waiting at the approval gate.

## Friction

**The approval gate fired as designed.** Copilot presented the complete diff
and asked whether to apply it or revise it first, rather than writing to disk
unilaterally. This is the behaviour the prompt asked for — *"Show me the
changes as a diff before applying anything"* — so it is recorded as the gate
working, not as an obstacle. It is the one human decision this stage is
supposed to cost.

The proposed diff as presented at that gate is captured verbatim in
`01b-copilot-proposed.diff`.

## Two things the proposed diff shows

### 1. The defect was present at the approval gate

The proposed diff already contained the test that Stage 3 later caught as
broken:

```java
+        @Test
+        @DisplayName("reminders are returned in due-date order")
+        void remindersAreSortedByDueDate() {
+            manager.addTask("Later", LocalDate.of(2026, 10, 2), 2);
+            manager.addTask("Sooner", LocalDate.of(2026, 9, 28), 3);
+
+            assertEquals(List.of("Sooner", "Later"),
+                    manager.tasksNeedingReminder(LocalDate.of(2026, 9, 28))
+                            .stream().map(Task::getName).toList());
+        }
```

`Later` is due 2026-10-02 with 2 days' lead, so its reminder window is
`[2026-09-30, 2026-10-02]`, which does not contain the 2026-09-28 date under
test. The expectation of `[Sooner, Later]` is therefore wrong, and the
assertion contradicts the reminder rule in section 2 of the spec Copilot was
implementing.

It passed human review. That is not a criticism of the review — the assertion
reads as entirely plausible, and verifying it requires holding two dates and an
inclusive window in your head. It is the argument for why the CLI stage exists:
**a diff review checks intent, and only execution checks arithmetic.**

### 2. Copilot edited Artifact A, and the diff proves it

The proposed diff includes changes to `docs/features/task-due-dates.md`,
replacing each `TODO(stage-2)` marker with a prose line:

```
-// TODO(stage-2): bind to the real TaskManager ...
+Implemented in `TaskManager`; the existing task list and logger are reused.
```

Those lines landed inside the ```java fence, leaving the code block malformed.
Artifact A was restored in Stage 3, because it is Stage 1's output and editing
it destroys the record of what Stage 1 produced. The cause is prompt ambiguity,
not tool error — see the friction log in the report.

## Discrepancy between proposed and applied

The applied state differs from the proposed diff in one place:
`TaskManagerTest.addReportsInvalidValues` gained two assertions
(`addTask(null, null, null)` and `addTask("   ", null, null)`) that the proposed
diff does not contain. File modification times show a second write to the test
files at 17:16:47, after the first Stage 3 verification had already begun at
17:15:35.

Whether that came from a revise cycle at the gate or a second agent pass is not
recorded, so it is not claimed either way. The consequence is recorded: the
working tree carried no completion signal, Stage 3's first verification measured
a tree that no longer existed, and the verification had to be re-run against the
settled state before anything was committed.
