#!/usr/bin/env bash
# Flyway squash 검증용 비교 스크립트
#
# 사용법:
#   compare.sh <A-label> <B-label>
#     예: compare.sh A-dev B-new
#
# 입력 전제 (같은 디렉토리에 존재):
#   구조 덤프: <label>/01_tables.csv ... 06_triggers.csv
#   텍스트 덤프: <label>.normalized.sql (선택 — 있으면 참고용 diff)
#
# 출력: 각 CSV 의 diff 를 ./compare-out/<label>/ 에 저장. 전체 합 0 byte 면 통과.
# Exit code: 0 통과, 1 구조 diff 있음, 2 인자 오류.

set -euo pipefail

A="${1:?A label required}"
B="${2:?B label required}"
BASE="${BASE:-$(pwd)}"
OUT="${OUT:-${BASE}/compare-out}"

mkdir -p "$OUT"
rm -f "$OUT"/*.diff 2>/dev/null || true

if [[ ! -d "${BASE}/${A}" || ! -d "${BASE}/${B}" ]]; then
  echo "ERROR: structural dump directories missing: ${BASE}/${A} or ${BASE}/${B}" >&2
  exit 2
fi

fail=0
total_bytes=0

for f in 01_tables 02_columns 03_constraints 04_indexes 05_sequences 06_triggers; do
  a="${BASE}/${A}/${f}.csv"
  b="${BASE}/${B}/${f}.csv"
  out="${OUT}/${f}.diff"
  if [[ ! -f "$a" || ! -f "$b" ]]; then
    echo "[compare] MISSING: $a or $b"
    fail=1
    continue
  fi
  if diff -u "$a" "$b" > "$out"; then
    echo "[compare] OK     ${f}"
    rm -f "$out"
  else
    size=$(wc -c < "$out")
    total_bytes=$((total_bytes + size))
    echo "[compare] DIFF   ${f}  (${size} bytes) -> ${out}"
    fail=1
  fi
done

# 텍스트 dump 비교 (선택)
if [[ -f "${BASE}/${A}.normalized.sql" && -f "${BASE}/${B}.normalized.sql" ]]; then
  out="${OUT}/text.diff"
  if diff -u "${BASE}/${A}.normalized.sql" "${BASE}/${B}.normalized.sql" > "$out"; then
    echo "[compare] OK     text.normalized"
    rm -f "$out"
  else
    size=$(wc -c < "$out")
    echo "[compare] DIFF   text.normalized  (${size} bytes) -> ${out}  (참고용, 게이트 아님)"
  fi
fi

echo
if (( fail == 0 )); then
  echo "[compare] RESULT: PASS  (structural diff total = 0 bytes)"
  exit 0
else
  echo "[compare] RESULT: FAIL  (structural diff total = ${total_bytes} bytes)"
  exit 1
fi
