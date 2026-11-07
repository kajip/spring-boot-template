#!/usr/bin/env bash
# Stop hook: regenerates doc/ and runs ./gradlew check in the Dev Container when the working
# tree has uncommitted changes that have not been checked yet. Exit code 2 sends the failure
# to Claude.
set -uo pipefail

readonly MAX_ATTEMPTS=3

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
git rev-parse --verify --quiet HEAD >/dev/null || exit 0
[ -n "$(git status --porcelain)" ] || exit 0

state_dir=$(git rev-parse --git-path claude-check)
mkdir -p "$state_dir"

notify_user() {
  printf '{"systemMessage": "%s"}\n' "$1"
  exit 0
}

fingerprint() {
  {
    git rev-parse HEAD
    git diff --binary HEAD
    git ls-files --others --exclude-standard | while IFS= read -r file; do
      printf '%s ' "$file"
      git hash-object -- "$file"
    done
  } | git hash-object --stdin
}

current=$(fingerprint)
[ "$current" = "$(cat "$state_dir/passed" 2>/dev/null)" ] && exit 0
[ "$current" = "$(cat "$state_dir/failed" 2>/dev/null)" ] &&
  notify_user "check は同じ内容で失敗済みのため再実行を省略しました。"

# A new turn starts a new retry budget; only hook-driven retries count against it.
grep -q '"stop_hook_active": *true' <<<"$(cat)" || echo 0 >"$state_dir/attempts"
attempts=$(cat "$state_dir/attempts" 2>/dev/null || echo 0)
[ "$attempts" -lt "$MAX_ATTEMPTS" ] ||
  notify_user "check の修正を ${MAX_ATTEMPTS} 回試みたため自動実行を中断しました。未検証の変更が残っています。"

npx -y @devcontainers/cli up --workspace-folder . >/dev/null 2>&1 ||
  notify_user "Dev Container を起動できないため check を省略しました。"

output=$(npx -y @devcontainers/cli exec --workspace-folder . ./gradlew --console=plain generateDocs check --continue 2>&1)
status=$?
# Another container is using the shared Gradle cache: not a code failure, so record nothing
# and let the next stop retry.
if [ "$status" -ne 0 ] && grep -q 'Timeout waiting to lock' <<<"$output"; then
  notify_user "Gradle キャッシュが他のコンテナで使用中のため check を完了できませんでした。次回の応答終了時に再実行します。"
fi
# generateDocs may have rewritten doc/, so record the state after the run.
current=$(fingerprint)
if [ "$status" -eq 0 ]; then
  echo "$current" >"$state_dir/passed"
  rm -f "$state_dir/failed"
  exit 0
fi

echo "$current" >"$state_dir/failed"
echo $((attempts + 1)) >"$state_dir/attempts"
{
  echo "./gradlew check が失敗しました。原因を修正してください。"
  printf '%s\n' "$output" | grep -vE -e '^> Task .*(UP-TO-DATE|NO-SOURCE)$' -e '^$' | tail -n 80
} >&2
exit 2
