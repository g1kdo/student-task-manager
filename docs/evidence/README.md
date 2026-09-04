# Evidence

Everything here is generated from the current repository state and can be
reproduced with the commands each file records.

| File | What it evidences |
|------|-------------------|
| `test-run.txt` | `./mvnw clean verify` running all 69 tests green, with the coverage gate met |
| `coverage.txt` | Per-class JaCoCo coverage, and the threshold the build enforces |
| `pipeline-fails-on-failing-test.txt` | The build failing when one assertion was deliberately inverted, then restored — the pipeline can fail |
| `demo-session.txt` | A full console session covering all five user stories, an out-of-range task number and an invalid menu choice |
| `ci-run.png` | GitHub Actions run for the current pipeline (add after pushing; see below) |

## Reproducing

```bash
./mvnw clean verify                    # tests + coverage report + coverage gate
./mvnw package -DskipTests             # build the runnable jar
java -jar target/student-task-manager.jar
```

## CI screenshots

`ci-run.png` is captured from the GitHub Actions run for the pushed branch:
open the repository's **Actions** tab, pick the most recent **CI** run and
screenshot the expanded job. The archived screenshots of the first attempt's
pipeline are kept in `../archive/first-attempt/` for comparison.
