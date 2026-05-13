#!/usr/bin/env bash
#
# sobject-list.txt 파서 (lib).
# 출력: stdout 으로 `<entity>|<sf_api>|<table>|<fk_mapping>` 한 줄씩.
#   주석/빈 줄 제거 + ONLY 필터 적용.
#
set -euo pipefail

LIST_FILE="${SF_MIGRATE_DIR}/sobject-list.txt"

awk -F'|' -v only="${SF_MIGRATE_ONLY:-}" '
  BEGIN {
    n = split(only, arr, ",")
    for (i = 1; i <= n; i++) {
      gsub(/[ \t]+/, "", arr[i])
      if (arr[i] != "") filter[arr[i]] = 1
    }
  }
  /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
  {
    entity = $1
    gsub(/[ \t]+/, "", entity)
    if (entity == "") next
    if (length(filter) > 0 && !(entity in filter)) next
    print
  }
' "$LIST_FILE"
