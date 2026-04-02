#!/bin/bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
probe_text="brief-clipboard-probe-$(date +%s)-$$"

read_clipboard() {
  case "${CLIPBOARD_BACKEND}" in
    pbcopy)
      pbpaste
      ;;
    wl-clipboard)
      wl-paste --no-newline 2>/dev/null || wl-paste
      ;;
    xclip)
      xclip -o -selection clipboard
      ;;
    xsel)
      xsel --clipboard --output
      ;;
    *)
      echo "Unsupported clipboard backend: ${CLIPBOARD_BACKEND}" >&2
      return 1
      ;;
  esac
}

write_clipboard() {
  local text="$1"
  case "${CLIPBOARD_BACKEND}" in
    pbcopy)
      printf '%s' "$text" | pbcopy
      ;;
    wl-clipboard)
      printf '%s' "$text" | wl-copy
      ;;
    xclip)
      printf '%s' "$text" | xclip -selection clipboard
      ;;
    xsel)
      printf '%s' "$text" | xsel --clipboard --input
      ;;
    *)
      echo "Unsupported clipboard backend: ${CLIPBOARD_BACKEND}" >&2
      return 1
      ;;
  esac
}

detect_clipboard_backend() {
  case "$(uname -s)" in
    Darwin)
      command -v pbcopy >/dev/null
      command -v pbpaste >/dev/null
      CLIPBOARD_BACKEND="pbcopy"
      ;;
    Linux)
      if command -v wl-copy >/dev/null && command -v wl-paste >/dev/null; then
        CLIPBOARD_BACKEND="wl-clipboard"
      elif command -v xclip >/dev/null; then
        CLIPBOARD_BACKEND="xclip"
      elif command -v xsel >/dev/null; then
        CLIPBOARD_BACKEND="xsel"
      else
        echo "No supported Linux clipboard tool found (need wl-clipboard, xclip, or xsel)." >&2
        exit 1
      fi
      ;;
    *)
      echo "Clipboard smoke test is only supported on macOS and Linux." >&2
      exit 1
      ;;
  esac
}

restore_clipboard() {
  if [[ -n "${previous_clipboard+x}" ]]; then
    write_clipboard "${previous_clipboard}" >/dev/null 2>&1 || true
  fi
}

detect_clipboard_backend
trap restore_clipboard EXIT

previous_clipboard="$(read_clipboard 2>/dev/null || true)"

probe_class="${repo_dir}/build/classes/java/test/com/williamcallahan/chatclient/ClipboardSmokeProbe.class"
if [[ ! -f "${probe_class}" ]]; then
  echo "Missing compiled clipboard probe: ${probe_class}" >&2
  echo "Run './gradlew testClasses' or 'make test-clipboard-local' from the repo root." >&2
  exit 1
fi

shopt -s nullglob
installed_jars=("${repo_dir}"/build/install/brief/lib/*.jar)
shopt -u nullglob
if [[ ${#installed_jars[@]} -eq 0 ]]; then
  echo "No installed Brief jars found under build/install/brief/lib." >&2
  echo "Run 'make build' first." >&2
  exit 1
fi

classpath="${repo_dir}/build/classes/java/test"
for jar_path in "${installed_jars[@]}"; do
  classpath="${classpath}:${jar_path}"
done

copy_result="$(
  JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED" \
    java -cp "${classpath}" com.williamcallahan.chatclient.ClipboardSmokeProbe "${probe_text}"
)"
if [[ "${copy_result}" != "true" ]]; then
  echo "Clipboard.tryCopy returned '${copy_result}' instead of 'true'." >&2
  exit 1
fi

actual_clipboard="$(read_clipboard)"
if [[ "${actual_clipboard}" != "${probe_text}" ]]; then
  echo "Clipboard smoke test failed." >&2
  echo "Expected: ${probe_text}" >&2
  echo "Actual:   ${actual_clipboard}" >&2
  exit 1
fi

echo "Clipboard smoke test passed via ${CLIPBOARD_BACKEND}."
