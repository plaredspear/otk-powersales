#!/usr/bin/env python3
"""
SObject API name → entity 메타 JSON 추출.

backend/src/main/kotlin 의 @SFObject 어노테이션이 부착된 entity 파일을 파싱하여
SOQL 생성 / CSV transform / Phase 2 FK 연결에 필요한 메타데이터를 stdout JSON 으로 반환한다.

사용:
  entity-meta.py <SObject_API_Name>
  entity-meta.py --list-all                  # 모든 @SFObject entity (FK 의존성 순서 정렬)
  entity-meta.py --list-all --tables-only    # 의존성 순서대로 sobject 명만 출력

출력 (단일 sobject):
  {
    "entity_class": "Organization",
    "kotlin_file": "/abs/path/Organization.kt",
    "sobject": "Org__c",
    "schema": "powersales",
    "table": "organization",
    "fields": [
      {"sf": "Id", "db": "sfid"},
      {"sf": "CostCenterLevel2__c", "db": "cc_cd2"},
      ...
      {"sf": "CreatedDate", "db": "created_at"},
      {"sf": "LastModifiedDate", "db": "updated_at"}
    ],
    "fk_mappings": [
      {"src_sfid": "owner_sfid", "src_fk": "owner_id",
       "target_class": "Employee", "target_table": "employee"}
    ]
  }
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

# /backend/scripts/sf-migrate/lib/entity-meta.py 기준
SCRIPT_DIR = Path(__file__).resolve().parent
BACKEND_ROOT = SCRIPT_DIR.parents[2]  # /backend
KOTLIN_ROOT = BACKEND_ROOT / "src" / "main" / "kotlin"

RE_SFOBJECT = re.compile(r'@SFObject\(\s*"([^"]+)"\s*\)')
RE_TABLE = re.compile(r'@Table\(\s*name\s*=\s*"([^"]+)"')
RE_CLASS = re.compile(r'class\s+([A-Z][A-Za-z0-9_]*)\s*\(')
RE_EXTENDS_BASE = re.compile(r'\)\s*:\s*BaseEntity\s*\(')
RE_SFID_COLUMN = re.compile(r'@Column\(\s*name\s*=\s*"sfid"')

# @SFField + 중간 어노테이션 (HCColumn / Comment / Convert / Enumerated 등) + @Column
RE_SFFIELD_AND_COLUMN = re.compile(
    r'@SFField\(\s*"([^"]+)"\s*\)\s*'
    r'(?:@[A-Za-z_][A-Za-z0-9_]*\s*(?:\([^)]*\))?\s*)*'
    r'@Column\(\s*name\s*=\s*"([^"]+)"'
)

# @JoinColumn + 직후 ManyToOne 타입 ( val/var <name>: <Class> )
RE_JOINCOLUMN = re.compile(
    r'@JoinColumn\(\s*name\s*=\s*"([^"]+)"[^)]*\)\s*\n'
    r'\s*(?:var|val)\s+\w+\s*:\s*([A-Z][A-Za-z0-9_]*)'
)

# BaseEntity (BaseEntity.kt) 의 @SFField 매핑 — 모든 BaseEntity 상속 entity 가 공유
BASE_ENTITY_FIELDS = [
    {"sf": "CreatedDate", "db": "created_at"},
    {"sf": "LastModifiedDate", "db": "updated_at"},
]

_class_to_table_cache: dict[str, str | None] = {}


def find_table_for_class(class_name: str) -> str | None:
    """다른 .kt 파일에서 `class <Name>` 정의를 찾아 @Table(name=...) 추출."""
    if class_name in _class_to_table_cache:
        return _class_to_table_cache[class_name]
    pattern = re.compile(rf"class\s+{re.escape(class_name)}\b")
    for kt_file in KOTLIN_ROOT.rglob("*.kt"):
        text = kt_file.read_text(encoding="utf-8")
        if pattern.search(text):
            table_m = RE_TABLE.search(text)
            table = table_m.group(1) if table_m else None
            _class_to_table_cache[class_name] = table
            return table
    _class_to_table_cache[class_name] = None
    return None


def parse_entity_file(path: Path) -> dict | None:
    text = path.read_text(encoding="utf-8")

    sobject_m = RE_SFOBJECT.search(text)
    table_m = RE_TABLE.search(text)
    class_m = RE_CLASS.search(text)
    if not (sobject_m and table_m and class_m):
        return None

    fields: list[dict[str, str]] = []

    # SF Id → sfid (sfid 컬럼 있을 때만)
    if RE_SFID_COLUMN.search(text):
        fields.append({"sf": "Id", "db": "sfid"})

    # @SFField + @Column 페어
    seen_db_cols = set(f["db"] for f in fields)
    for m in RE_SFFIELD_AND_COLUMN.finditer(text):
        sf, db = m.group(1), m.group(2)
        if db in seen_db_cols:
            continue
        fields.append({"sf": sf, "db": db})
        seen_db_cols.add(db)

    # BaseEntity 상속 시 created_at/updated_at 자동 포함
    if RE_EXTENDS_BASE.search(text):
        for bf in BASE_ENTITY_FIELDS:
            if bf["db"] not in seen_db_cols:
                fields.append(bf)
                seen_db_cols.add(bf["db"])

    # FK 매핑 추정: *_sfid 컬럼의 prefix 와 @JoinColumn 매칭
    join_columns = {m.group(1): m.group(2) for m in RE_JOINCOLUMN.finditer(text)}
    fk_mappings: list[dict[str, str]] = []
    sfid_cols = [f["db"] for f in fields if f["db"].endswith("_sfid") and f["db"] != "sfid"]
    for src_sfid in sfid_cols:
        prefix = src_sfid[: -len("_sfid")]
        fk_col = f"{prefix}_id"
        if fk_col not in join_columns:
            continue
        target_class = join_columns[fk_col]
        target_table = find_table_for_class(target_class)
        if not target_table:
            continue
        fk_mappings.append({
            "src_sfid": src_sfid,
            "src_fk": fk_col,
            "target_class": target_class,
            "target_table": target_table,
        })

    return {
        "entity_class": class_m.group(1),
        "kotlin_file": str(path),
        "sobject": sobject_m.group(1),
        "schema": "powersales",
        "table": table_m.group(1),
        "fields": fields,
        "fk_mappings": fk_mappings,
    }


def find_entity_by_sobject(api_name: str) -> Path | None:
    pattern = re.compile(rf'@SFObject\(\s*"{re.escape(api_name)}"\s*\)')
    for kt_file in KOTLIN_ROOT.rglob("*.kt"):
        text = kt_file.read_text(encoding="utf-8")
        if pattern.search(text):
            return kt_file
    return None


def list_all_entities() -> list[dict]:
    results: list[dict] = []
    for kt_file in KOTLIN_ROOT.rglob("*.kt"):
        text = kt_file.read_text(encoding="utf-8")
        if RE_SFOBJECT.search(text):
            meta = parse_entity_file(kt_file)
            if meta:
                results.append(meta)
    return topological_sort(results)


def topological_sort(entities: list[dict]) -> list[dict]:
    by_table = {e["table"]: e for e in entities}
    visited: set[str] = set()
    order: list[dict] = []

    def visit(entity: dict) -> None:
        if entity["table"] in visited:
            return
        visited.add(entity["table"])
        for fk in entity["fk_mappings"]:
            tgt = by_table.get(fk["target_table"])
            if tgt is not None:
                visit(tgt)
        order.append(entity)

    for e in sorted(entities, key=lambda x: x["sobject"]):
        visit(e)
    return order


def main() -> None:
    p = argparse.ArgumentParser(description="SObject API name → entity 메타 JSON")
    p.add_argument("sobject", nargs="?", help="SObject API name (예: Org__c)")
    p.add_argument("--list-all", action="store_true",
                   help="모든 @SFObject entity 메타 출력 (FK 의존성 순서 정렬)")
    p.add_argument("--tables-only", action="store_true",
                   help="--list-all 과 함께 사용 — sobject API name 만 한 줄씩 출력")
    args = p.parse_args()

    if args.list_all:
        metas = list_all_entities()
        if args.tables_only:
            for m in metas:
                print(m["sobject"])
        else:
            json.dump(metas, sys.stdout, ensure_ascii=False, indent=2)
        return

    if not args.sobject:
        p.error("SObject API name 또는 --list-all 중 하나가 필요합니다.")

    kt_file = find_entity_by_sobject(args.sobject)
    if not kt_file:
        sys.stderr.write(
            f'ERROR: @SFObject("{args.sobject}") 에 매핑된 entity 를 찾을 수 없습니다.\n'
        )
        sys.exit(1)

    meta = parse_entity_file(kt_file)
    if not meta:
        sys.stderr.write(f"ERROR: {kt_file} 메타 추출 실패.\n")
        sys.exit(1)

    json.dump(meta, sys.stdout, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
