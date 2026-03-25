-- tmp_claim 테이블에 FK 컬럼 추가 (account_id, employee_id, product_id)
ALTER TABLE salesforce2.tmp_claim ADD COLUMN account_id BIGINT;
ALTER TABLE salesforce2.tmp_claim ADD COLUMN employee_id BIGINT;
ALTER TABLE salesforce2.tmp_claim ADD COLUMN product_id BIGINT;
