#!/usr/bin/env bash
# Stage 3 capture helper.
#
# Records each command and its real output, with a timestamp and exit code,
# into 01-session.log. It captures; it does not decide. The fixes in Stage 3
# have to come from the agent reading actual failures, so nothing here
# pre-scripts a remedy — a wrapper that "knew" the answer would make the stage
# a performance rather than an execution.
#
# Usage, from the repository root:
#   docs/lab3/artefacts/stage3/capture.sh ./mvnw -B clean verify
#
set -uo pipefail

LOG_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG="$LOG_DIR/01-session.log"

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <command> [args...]" >&2
  exit 2
fi

# Header, written once per session.
if [ ! -s "$LOG" ]; then
  {
    echo "Stage 3 session log — CLI UX (Claude Code)"
    echo "Repository: $(git rev-parse --show-toplevel 2>/dev/null || echo '?')"
    echo "Branch:     $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
    echo "Base commit at session start: $(git rev-parse --short HEAD 2>/dev/null || echo '?')"
    echo "Started:    $(date -u '+%Y-%m-%d %H:%M:%S UTC')"
    echo
    echo "Every command below was actually run. Output is verbatim."
    echo "========================================================================"
  } > "$LOG"
fi

START_EPOCH=$(date +%s)
{
  echo
  echo "------------------------------------------------------------------------"
  echo "\$ $*"
  echo "  at $(date -u '+%H:%M:%S UTC')"
  echo "------------------------------------------------------------------------"
} >> "$LOG"

# Run it, streaming to the terminal and appending to the log.
"$@" 2>&1 | tee -a "$LOG"
RC=${PIPESTATUS[0]}

ELAPSED=$(( $(date +%s) - START_EPOCH ))
{
  echo
  echo "[exit=$RC, ${ELAPSED}s]"
} >> "$LOG"

exit "$RC"
