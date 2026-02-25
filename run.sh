#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PORT="${1:-2323}"

"$ROOT_DIR/build.sh"

exec java -jar "$ROOT_DIR/build/rps-server.jar" --port "$PORT"
