# Evidence

Everything here is generated from the current repository state and can be
reproduced with the commands each file records.

| File | What it evidences |
|------|-------------------|
| `test-run.txt` | `./mvnw clean verify` running all 69 tests green, with the coverage gate met |
| `coverage.txt` | Per-class JaCoCo coverage, and the threshold the build enforces |
| `ci-runs.txt` | Every GitHub Actions run with its result — the new pipeline failing on a real defect and going green once fixed, and the old pipeline reporting success on the commit that disabled its own testing |
| `pipeline-fails-on-failing-test.txt` | The build failing when one assertion was deliberately inverted, then restored — a failing test fails the build |
| `demo-session.txt` | A full console session covering all five user stories, an out-of-range task number and an invalid menu choice |
| `ci-run.png` | Optional screenshot of a green Actions run (see below) |

## Reproducing

```bash
./mvnw clean verify                    # tests + coverage report + coverage gate
./mvnw package -DskipTests             # build the runnable jar
java -jar target/student-task-manager.jar
```

The run table in `ci-runs.txt` comes from the public Actions API:

```bash
curl -s "https://api.github.com/repos/g1kdo/student-task-manager/actions/runs?per_page=8"
```

## CI screenshots

`ci-runs.txt` already records every run and its result, so a screenshot is
optional. To add one anyway: open the repository's **Actions** tab, pick the
most recent green **CI** run, screenshot the expanded job and save it here as
`ci-run.png`, then uncomment the `figure` block in the DevOps section of
`../report/assessment-report.tex`.

The archived screenshots of the first attempt's pipeline are kept in
`../archive/first-attempt/` for comparison.
