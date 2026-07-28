#!/usr/bin/env bash

set -euo pipefail

paths=()

while [[ $# -gt 0 ]]; do
  case $1 in
    -path)
      IFS=',' read -r -a paths <<< "$2"
      shift 2
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

if [[ ${#paths[@]} -eq 0 ]]; then
  echo "Usage: $0 -path loader[,loader...]" >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to generate the GitHub Actions matrix" >&2
  exit 1
fi

if [[ -z "${GITHUB_OUTPUT:-}" ]]; then
  echo "GITHUB_OUTPUT is not set" >&2
  exit 1
fi

normalize_support_versions() {
  local value="$1"
  value="${value//$'\r'/}"
  value="${value//$'\n'/}"

  if [[ "$value" == \[*\] ]]; then
    value="${value:1:${#value}-2}"
    value="${value//,/$'\n'}"
  fi

  printf '%s\n' "$value" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | sed '/^$/d'
}

matrix_file=$(mktemp)
trap 'rm -f "$matrix_file"' EXIT

# 遍历每个路径
for path in "${paths[@]}"; do
  if [[ ! -d "$path" ]]; then
    echo "Loader directory does not exist: $path" >&2
    exit 1
  fi

  for folder in "$path"/*; do
    [[ -d "$folder" ]] || continue
    folderName=$(basename "$folder")
    if [[ "$folderName" != "origin" ]]; then
      # 提取 mc-version 和 mc-loader 信息
      mcVersion="${folderName//$path-/}"
      mcLoader="$path"
      supportVersionFile="$mcLoader/$mcLoader-$mcVersion/support_version.txt"
      if [[ -f "$supportVersionFile" ]]; then
        supportVersion=$(normalize_support_versions "$(<"$supportVersionFile")")
      else
        supportVersion="$mcVersion"
      fi

      if [ "$mcLoader" == "fabric" ]; then
        publishLoaders="fabric quilt"
      else
        publishLoaders="$mcLoader"
      fi
      jq -cn \
        --arg mcVersion "$mcVersion" \
        --arg mcLoader "$mcLoader" \
        --arg publishLoaders "$publishLoaders" \
        --arg publishVersion "$supportVersion" \
        '{"mc-version": $mcVersion, "mc-loader": $mcLoader, "publish-loaders": $publishLoaders, "publish-version": $publishVersion}' \
        >> "$matrix_file"
    fi
  done
done

json=$(jq -cs '{config: .}' "$matrix_file")

printf 'matrix=%s\n' "$json" >> "$GITHUB_OUTPUT"
