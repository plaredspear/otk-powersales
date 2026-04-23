-- Flyway squash: flyway_schema_history 를 새 V1 한 줄로 교체
--
-- 전제:
--   1. backend/src/main/resources/db/migration/ 에 새 V1__init_schema.sql 만 남아 있음
--   2. dev-db 에 이미 V1~V91 이 적용되어 있고, 스키마 검증(compare.sh) 통과
--   3. 이 스크립트 실행 전 전체 DB 백업 완료 (pg_dumpall 또는 RDS snapshot)
--
-- 실행 후 절차:
--   ./gradlew flywayRepair     -- checksum 을 실제 V1 파일 기준으로 갱신
--   ./gradlew flywayValidate   -- 0 error 여야 함
--   ./gradlew flywayInfo       -- V1 만 Success 로 표시되어야 함

\set ON_ERROR_STOP on

BEGIN;

-- 이미 백업이 있으면 중복 방지
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'salesforce2'
      AND table_name = 'flyway_schema_history_backup_20260423'
  ) THEN
    RAISE EXCEPTION 'Backup table already exists. Aborting to avoid overwrite.';
  END IF;
END $$;

-- 1. 기존 history 전체 백업
CREATE TABLE salesforce2.flyway_schema_history_backup_20260423 AS
  SELECT * FROM salesforce2.flyway_schema_history;

-- 2. 현재 행 개수 확인용 출력 (트랜잭션 내)
DO $$
DECLARE
  cnt INT;
BEGIN
  SELECT count(*) INTO cnt FROM salesforce2.flyway_schema_history_backup_20260423;
  RAISE NOTICE 'Backed up % rows into flyway_schema_history_backup_20260423', cnt;
END $$;

-- 3. 기존 history 비우기
DELETE FROM salesforce2.flyway_schema_history;

-- 4. 새 V1 한 줄 삽입 (checksum 은 flywayRepair 가 재계산)
INSERT INTO salesforce2.flyway_schema_history
  (installed_rank, version, description, type, script, checksum,
   installed_by, installed_on, execution_time, success)
VALUES
  (1, '1', 'init schema', 'SQL', 'V1__init_schema.sql', NULL,
   current_user, NOW(), 0, TRUE);

COMMIT;

\echo '--- Post-swap verification ---'

SELECT installed_rank, version, script, success, checksum
FROM salesforce2.flyway_schema_history
ORDER BY installed_rank;

\echo
\echo 'Next steps:'
\echo '  1. cd backend && ./gradlew flywayRepair'
\echo '  2. ./gradlew flywayValidate'
\echo '  3. ./gradlew flywayInfo'
\echo '  4. ./gradlew bootRun   (ddl-auto: validate 확인)'
