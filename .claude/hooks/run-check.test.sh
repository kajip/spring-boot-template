#!/usr/bin/env bash
# Tests run-check.sh against a throwaway repository, with npx replaced by a stub.
# Usage: .claude/hooks/run-check.test.sh
set -u

hook="$(cd "$(dirname "$0")" && pwd)/run-check.sh"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
failures=0

# The stub stands in for the Dev Container CLI. STUB_STATUS, STUB_OUTPUT and STUB_TOUCH
# control what `exec ./gradlew ...` does; every call is logged to calls.log.
mkdir "$work/bin"
cat >"$work/bin/npx" <<'STUB'
#!/usr/bin/env bash
echo "$*" >>"$STUB_LOG"
for arg in "$@"; do [ "$arg" = up ] && exit 0; done
[ -n "${STUB_TOUCH:-}" ] && echo generated >>"$STUB_TOUCH"
printf '%s\n' "${STUB_OUTPUT:-BUILD SUCCESSFUL}"
exit "${STUB_STATUS:-0}"
STUB
chmod +x "$work/bin/npx"

git init -q --bare "$work/origin.git"
git init -q -b main "$work/repo"
cd "$work/repo" || exit 1
git config user.name test
git config user.email test@example.invalid
mkdir -p doc/autogen
echo a >app.txt
echo diagram >doc/autogen/diagram.md
git add -A
git commit -qm base
git remote add origin "$work/origin.git"
git push -q origin main

# run <name> <expected exit> <expected checks run> [stdin] [env assignments...]
run() {
  local name=$1 want_exit=$2 want_calls=$3 input=${4:-'{}'}
  shift 4 2>/dev/null || shift $#
  : >"$work/calls.log"
  local output code calls
  output=$(echo "$input" | env PATH="$work/bin:$PATH" STUB_LOG="$work/calls.log" \
    CLAUDE_PROJECT_DIR="$work/repo" "$@" "$hook" 2>&1)
  code=$?
  calls=$(grep -c 'gradlew' "$work/calls.log")
  if [ "$code" = "$want_exit" ] && [ "$calls" = "$want_calls" ]; then
    echo "ok   $name"
  else
    echo "FAIL $name: exit=$code (want $want_exit) checks=$calls (want $want_calls)"
    printf '%s\n' "$output" | sed 's/^/     /' | head -5
    failures=$((failures + 1))
  fi
  LAST_OUTPUT=$output
}

run "clean main is skipped" 0 0

echo b >>app.txt
run "uncommitted change is checked" 0 1
run "same content is skipped" 0 0

git switch -q -c feature
git commit -qam "committed change"
run "committed but not on origin/main is checked" 0 1
run "same commit is skipped after it passed" 0 0

echo c >>app.txt
git commit -qam "another commit"
run "regenerated documents left uncommitted are reported" 2 1 '{}' STUB_TOUCH=doc/autogen/diagram.md
case "$LAST_OUTPUT" in
  *doc/autogen*) echo "ok   report names doc/autogen" ;;
  *) echo "FAIL report names doc/autogen"; failures=$((failures + 1)) ;;
esac
git commit -qam "commit regenerated documents"
run "committed regenerated documents pass" 0 1

echo d >>app.txt
git commit -qam "broken change"
run "failing check is reported" 2 1 '{}' STUB_STATUS=1 STUB_OUTPUT='> Task :test FAILED'
run "same failing content is not rerun" 0 0

echo e >>app.txt
git commit -qam "change during lock"
run "lock timeout is not recorded" 0 1 '{}' STUB_STATUS=1 STUB_OUTPUT='Timeout waiting to lock journal cache'
run "lock timeout is retried" 0 1 '{}' STUB_STATUS=1 STUB_OUTPUT='Timeout waiting to lock journal cache'

git push -q origin feature:main
git fetch -q origin
run "commit merged into origin/main is skipped" 0 0

if [ "$failures" -eq 0 ]; then
  echo "all tests passed"
else
  echo "$failures test(s) failed"
  exit 1
fi
