#!/usr/bin/env bash
#
# SF CSV 헤더를 DB 컬럼명으로 변환 + Formula 등 비매핑 컬럼 제거.
#
# 사용:
#   transform.sh <entity> <in_csv> <out_csv> <db_cols_var_name>
#
# 매핑 파일: mapping/<entity>.tsv  형식: <sf_field>\t<db_column>  (첫 행은 헤더)
#
# 출력 CSV:
#   1) 헤더: DB 컬럼명 (mapping 에서 매칭된 컬럼만 보존, 순서는 mapping 정의 순)
#   2) 데이터 행: 위 헤더 순서로 재배치
#
# 추가로 DB 컬럼 목록을 stdout 첫 줄에 콤마 구분으로 출력 (\copy 사용).
#
set -euo pipefail

ENTITY="${1:?entity 명 필수}"
IN_CSV="${2:?입력 CSV 필수}"
OUT_CSV="${3:?출력 CSV 필수}"

MAP_FILE="${SF_MIGRATE_DIR}/mapping/${ENTITY}.tsv"
if [[ ! -f "$MAP_FILE" ]]; then
  echo "ERROR: mapping sidecar 가 없습니다: $MAP_FILE" >&2
  exit 1
fi

python3 - "$IN_CSV" "$OUT_CSV" "$MAP_FILE" <<'PYEOF'
import csv
import sys

in_path, out_path, map_path = sys.argv[1:4]

# mapping 로드 (헤더 제외)
mapping = []  # list of (sf_field, db_column)
with open(map_path, newline="", encoding="utf-8") as f:
    reader = csv.reader(f, delimiter="\t")
    header_skipped = False
    for row in reader:
        if not row or row[0].startswith("#"):
            continue
        if not header_skipped:
            header_skipped = True
            continue
        if len(row) < 2:
            continue
        mapping.append((row[0].strip(), row[1].strip()))

sf_to_db = dict(mapping)
db_cols = [db for _, db in mapping]

with open(in_path, newline="", encoding="utf-8") as fin, \
     open(out_path, "w", newline="", encoding="utf-8") as fout:
    reader = csv.reader(fin)
    try:
        header = next(reader)
    except StopIteration:
        sys.exit("ERROR: 입력 CSV 가 비어 있습니다.")

    # SF 헤더 → 인덱스
    idx_of = {name: i for i, name in enumerate(header)}

    missing = [sf for sf in sf_to_db.keys() if sf not in idx_of]
    if missing:
        sys.stderr.write(f"WARN: SF 응답에 없는 필드 (skip): {missing}\n")

    out_indices = []  # (db_col, src_idx_or_None)
    for sf, db in mapping:
        out_indices.append((db, idx_of.get(sf)))

    writer = csv.writer(fout)
    writer.writerow([db for db, _ in out_indices])

    row_n = 0
    for row in reader:
        out_row = []
        for _db, idx in out_indices:
            if idx is None or idx >= len(row):
                out_row.append("")
            else:
                v = row[idx]
                # SF 의 truthy/falsy boolean 표현은 그대로 PG 가 수용
                out_row.append(v)
        writer.writerow(out_row)
        row_n += 1

    sys.stderr.write(f"[transform:{in_path}] rows={row_n} cols={len(out_indices)}\n")

# stdout 으로 DB 컬럼 목록 출력
print(",".join(db_cols))
PYEOF
