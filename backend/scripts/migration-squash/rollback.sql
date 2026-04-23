-- Flyway squash 롤백: 백업 테이블에서 flyway_schema_history 복원
--
-- 이 스크립트는 history-swap.sql 로 생성된 백업 테이블이 있을 때만 동작한다.
-- V파일 쪽 롤백은 별도로 git 에서 수행:
--   git checkout pre-flyway-squash-20260423 -- backend/src/main/resources/db/migration/

\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'salesforce2'
      AND table_name = 'flyway_schema_history_backup_20260423'
  ) THEN
    RAISE EXCEPTION 'Backup table flyway_schema_history_backup_20260423 not found. Cannot rollback.';
  END IF;
END $$;

-- 현재 history 비우기
DELETE FROM salesforce2.flyway_schema_history;

-- 백업에서 복원
INSERT INTO salesforce2.flyway_schema_history
  SELECT * FROM salesforce2.flyway_schema_history_backup_20260423;

COMMIT;

\echo '--- Post-rollback verification ---'

SELECT count(*) AS restored_rows FROM salesforce2.flyway_schema_history;

SELECT installed_rank, version, script, success
FROM salesforce2.flyway_schema_history
ORDER BY installed_rank
LIMIT 5;

\echo
\echo 'Remember: also restore V files via git:'
\echo '  git checkout pre-flyway-squash-20260423 -- backend/src/main/resources/db/migration/'
