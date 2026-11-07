#!/usr/bin/env bash
# Prints a one-hour GitHub App installation token for the agent. Never log its output.
set -euo pipefail

source "$(dirname "$0")/../.github/agent-app.env"
key="${AGENT_APP_KEY:-$HOME/.config/claude-code-coding-agent/key.pem}"

base64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

now=$(date +%s)
# GitHub rejects tokens issued ahead of its clock and those valid for over 10 minutes.
header=$(printf '{"alg":"RS256","typ":"JWT"}' | base64url)
payload=$(printf '{"iat":%d,"exp":%d,"iss":"%s"}' $((now - 60)) $((now + 540)) "$AGENT_APP_ID" | base64url)
signature=$(printf '%s.%s' "$header" "$payload" | openssl dgst -sha256 -sign "$key" | base64url)

curl -fsS -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $header.$payload.$signature" \
  "https://api.github.com/app/installations/$AGENT_APP_INSTALLATION_ID/access_tokens" |
  jq -er .token
