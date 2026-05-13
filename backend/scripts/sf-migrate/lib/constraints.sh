#!/usr/bin/env bash
#
# 대상 테이블의 FK + NOT NULL constraint 를 임시 DROP / 복원.
#
# 본 스크립트는 phase1-load.sh / phase2-link-fk.sh 가 호출한다. 단독 호출 시
# 환경변수 (DEV_OTK_PWRS_DB_PASSWORD 및 PGHOST/PGPORT/PGUSER/PGDATABASE) 는
# migrate.sh 의 상단 주석 참조.
#
# 사용:
#   constraints.sh save   <restore_sql_path>  <table1> [table2 ...]
#   constraints.sh drop   <table1> [table2 ...]
#   constraints.sh restore <restore_sql_path>
#
# save: pg_constraint + information_schema.columns 를 조회하여 복원 SQL 을 파일에 기록.
# drop: 위 대상의 FK constraint DROP + NOT NULL drop.
# restore: save 단계에서 기록한 SQL 을 그대로 psql 실행.
#
set -euo pipefail

ACTION="${1:?action 필수 (save|drop|restore)}"
shift

run_psql() {
  psql -v ON_ERROR_STOP=1 "$@"
}

case "$ACTION" in
  save)
    RESTORE_PATH="${1:?restore SQL 경로 필수}"
    shift
    if [[ $# -lt 1 ]]; then
      echo "ERROR: 대상 테이블이 비어 있습니다." >&2
      exit 1
    fi

    TABLES_LITERAL=$(printf "'%s'," "$@")
    TABLES_LITERAL="${TABLES_LITERAL%,}"

    # 1) FK constraint 복원 SQL 추출
    FK_SQL=$(run_psql -At -c "
      SELECT format(
        'ALTER TABLE %I.%I ADD CONSTRAINT %I %s;',
        n.nspname, c.relname, con.conname, pg_get_constraintdef(con.oid)
      )
      FROM pg_constraint con
      JOIN pg_class c ON c.oid = con.conrelid
      JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE con.contype = 'f'
        AND n.nspname = 'powersales'
        AND c.relname IN ($TABLES_LITERAL)
      ORDER BY n.nspname, c.relname, con.conname;
    ")

    # 2) NOT NULL 복원 SQL 추출
    NN_SQL=$(run_psql -At -c "
      SELECT format(
        'ALTER TABLE %I.%I ALTER COLUMN %I SET NOT NULL;',
        table_schema, table_name, column_name
      )
      FROM information_schema.columns
      WHERE table_schema = 'powersales'
        AND table_name IN ($TABLES_LITERAL)
        AND is_nullable = 'NO'
        AND column_default IS NULL  -- IDENTITY 컬럼 등 default 보유 컬럼은 ALTER 불요
      ORDER BY table_name, ordinal_position;
    ")

    {
      echo "-- 복원 SQL — 생성 시각: $(date -u +%FT%TZ)"
      echo "-- 대상 테이블: $*"
      echo ""
      echo "-- ── NOT NULL 복원 ──"
      echo "$NN_SQL"
      echo ""
      echo "-- ── FK constraint 복원 ──"
      echo "$FK_SQL"
    } > "$RESTORE_PATH"

    echo "[constraints] save → $RESTORE_PATH ($(echo "$FK_SQL" | grep -c . || true) FK, $(echo "$NN_SQL" | grep -c . || true) NN)"
    ;;

  drop)
    if [[ $# -lt 1 ]]; then
      echo "ERROR: 대상 테이블이 비어 있습니다." >&2
      exit 1
    fi

    TABLES_LITERAL=$(printf "'%s'," "$@")
    TABLES_LITERAL="${TABLES_LITERAL%,}"

    # 1) FK DROP
    FK_DROP_SQL=$(run_psql -At -c "
      SELECT format('ALTER TABLE %I.%I DROP CONSTRAINT %I;', n.nspname, c.relname, con.conname)
      FROM pg_constraint con
      JOIN pg_class c ON c.oid = con.conrelid
      JOIN pg_namespace n ON n.oid = c.relnamespace
      WHERE con.contype = 'f'
        AND n.nspname = 'powersales'
        AND c.relname IN ($TABLES_LITERAL);
    ")

    # 2) NOT NULL DROP
    NN_DROP_SQL=$(run_psql -At -c "
      SELECT format('ALTER TABLE %I.%I ALTER COLUMN %I DROP NOT NULL;', table_schema, table_name, column_name)
      FROM information_schema.columns
      WHERE table_schema = 'powersales'
        AND table_name IN ($TABLES_LITERAL)
        AND is_nullable = 'NO'
        AND column_default IS NULL;
    ")

    if [[ -n "$FK_DROP_SQL" ]]; then
      echo "$FK_DROP_SQL" | run_psql -q
    fi
    if [[ -n "$NN_DROP_SQL" ]]; then
      echo "$NN_DROP_SQL" | run_psql -q
    fi
    echo "[constraints] drop 완료 — $* ($(echo "$FK_DROP_SQL" | grep -c . || true) FK, $(echo "$NN_DROP_SQL" | grep -c . || true) NN)"
    ;;

  restore)
    RESTORE_PATH="${1:?restore SQL 경로 필수}"
    if [[ ! -f "$RESTORE_PATH" ]]; then
      echo "ERROR: 복원 SQL 파일이 없습니다: $RESTORE_PATH" >&2
      exit 1
    fi
    run_psql -q -f "$RESTORE_PATH"
    echo "[constraints] restore 완료 — $RESTORE_PATH"
    ;;

  *)
    echo "Unknown action: $ACTION" >&2
    exit 2
    ;;
esac
