-- Claim 엔티티 sfid 컬럼 추가 (Spec #515)
ALTER TABLE claim ADD COLUMN employee_sfid VARCHAR(18);
ALTER TABLE claim ADD COLUMN account_sfid VARCHAR(18);
