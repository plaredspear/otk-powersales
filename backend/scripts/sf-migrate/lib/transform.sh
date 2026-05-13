#!/usr/bin/env bash
#
# SF CSV 헤더를 DB 컬럼명으로 변환 + 매핑되지 않은 컬럼 제거.
#
# 사용:
#   transform.sh <in_csv> <out_csv> <meta_json>
#
# meta_json: entity-meta.py 출력의 fields 배열을 포함하는 JSON 문자열.
#            인자로 직접 받음 (호출자가 환경에 따라 파일/문자열로 전달 가능).
#            정확히는 `cat <meta.json>` 형식.
#
# 출력:
#   1) 헤더: DB 컬럼명 (메타 fields 의 db 순서)
#   2) 데이터 행: 위 순서로 재배치
#   3) stdout 마지막 줄: DB 컬럼 목록 (콤마 구분, \copy 사용)
#
set -euo pipefail

IN_CSV="${1:?입력 CSV 필수}"
OUT_CSV="${2:?출력 CSV 필수}"
META_JSON="${3:?meta JSON 필수}"

python3 - "$IN_CSV" "$OUT_CSV" "$META_JSON" <<'PYEOF'
import csv
import json
import sys

in_path, out_path, meta_json = sys.argv[1:4]
meta = json.loads(meta_json)
fields = meta["fields"]  # list of {"sf": ..., "db": ...}

sf_to_db = {f["sf"]: f["db"] for f in fields}
db_cols = [f["db"] for f in fields]

with open(in_path, newline="", encoding="utf-8") as fin, \
     open(out_path, "w", newline="", encoding="utf-8") as fout:
    reader = csv.reader(fin)
    try:
        header = next(reader)
    except StopIteration:
        sys.exit("ERROR: 입력 CSV 가 비어 있습니다.")

    idx_of = {name: i for i, name in enumerate(header)}

    missing = [sf for sf in sf_to_db.keys() if sf not in idx_of]
    if missing:
        sys.stderr.write(f"WARN: SF 응답에 없는 필드 (skip): {missing}\n")

    out_indices = [(f["db"], idx_of.get(f["sf"])) for f in fields]

    writer = csv.writer(fout)
    writer.writerow([db for db, _ in out_indices])

    row_n = 0
    for row in reader:
        out_row = []
        for _db, idx in out_indices:
            if idx is None or idx >= len(row):
                out_row.append("")
            else:
                out_row.append(row[idx])
        writer.writerow(out_row)
        row_n += 1

    sys.stderr.write(f"[transform] rows={row_n} cols={len(out_indices)}\n")

print(",".join(db_cols))
PYEOF
