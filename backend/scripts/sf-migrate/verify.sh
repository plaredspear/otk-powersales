#!/usr/bin/env bash
#
# 검증:
#   1) 적재 행수 (수기 대조용 출력)
#   2) sfid NULL 잔존 = 0
#   3) Orphan FK = 0 (Phase 2 적용 후, *_sfid IS NOT NULL AND <fk_id> IS NULL)
#
# exit 1 if any failure (sfid NULL > 0 or orphan > 0).
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

FAIL=0

echo ""
echo "── [Verify] 시작"

while IFS= read -r line; do
  IFS='|' read -r ENTITY SF_API TABLE FK_MAP <<<"$line"

  CNT=$(psql -At -c "SELECT COUNT(*) FROM powersales.${TABLE};")
  SFID_NULL=$(psql -At -c "SELECT COUNT(*) FROM powersales.${TABLE} WHERE sfid IS NULL;")

  printf "  %-30s rows=%-8s sfidNull=%s" "$TABLE" "$CNT" "$SFID_NULL"

  if [[ "$SFID_NULL" -ne 0 ]]; then
    printf "  [FAIL: sfid NULL]"
    FAIL=1
  fi

  if [[ -n "${FK_MAP:-}" && "${FK_MAP// }" != "" ]]; then
    IFS=';' read -ra MAPS <<<"$FK_MAP"
    for m in "${MAPS[@]}"; do
      m_clean="$(echo "$m" | tr -d ' ')"
      if [[ ! "$m_clean" =~ ^([a-zA-Z_]+)-\>([a-zA-Z_]+)\.sfid-\>([a-zA-Z_]+)$ ]]; then
        continue
      fi
      SRC_SFID="${BASH_REMATCH[1]}"
      SRC_FK="${BASH_REMATCH[3]}"

      ORPHAN=$(psql -At -c "
        SELECT COUNT(*) FROM powersales.${TABLE}
        WHERE ${SRC_SFID} IS NOT NULL AND ${SRC_FK} IS NULL;
      ")
      printf "  orphan(%s)=%s" "$SRC_FK" "$ORPHAN"
      if [[ "$ORPHAN" -ne 0 ]]; then
        printf " [FAIL]"
        FAIL=1
      fi
    done
  fi

  echo ""
done < <("$SCRIPT_DIR/lib/parse-list.sh")

echo ""
if [[ "$FAIL" -ne 0 ]]; then
  echo "[FAIL] verify 실패 — 위 출력 확인"
  exit 1
fi
echo "[OK] verify 통과"
