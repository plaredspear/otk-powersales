-- Product 테이블에서 Heroku Connect 동기화 메타데이터 컬럼 삭제 (#316)
ALTER TABLE salesforce2.product DROP COLUMN IF EXISTS _hc_lastop;
ALTER TABLE salesforce2.product DROP COLUMN IF EXISTS _hc_err;
