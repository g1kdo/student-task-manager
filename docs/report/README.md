# Consolidated assessment report

`assessment-report.tex` is the submission document: LaTeX source for the
consolidated Agile & DevOps report.

## Getting the PDF

**From CI (no local install).** Every push runs the `report` job in
`.github/workflows/ci.yml`, which compiles this file with a full TeX Live image
and publishes `assessment-report.pdf` as a build artefact. Open the run under
the repository's **Actions** tab and download **assessment-report-pdf**.

**Overleaf.** Upload `assessment-report.tex` to a new Overleaf project and press
Recompile. It needs no local files other than any screenshots referenced by
`\includegraphics`.

**Locally**, with a TeX distribution installed (MiKTeX or TeX Live):

```bash
cd docs/report
latexmk -pdf assessment-report.tex     # or: pdflatex twice, for the contents page
```

`pdflatex` must run twice so the table of contents and cross-references resolve.

## Adding a CI screenshot

The report has a commented-out `figure` block near the end of the DevOps
section. Once the pipeline has run green on GitHub, screenshot the run, save it
as `docs/evidence/ci-run.png`, and uncomment that block.
