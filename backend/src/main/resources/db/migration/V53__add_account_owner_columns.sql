-- Spec #644: Account 엔티티에 owner 차원 2개 추가.
--
-- 배경: DisplayWorkSchedule 풀 패턴 정합 — owner_sfid (HC sync buffer) +
-- owner_id (Employee FK, application 적재) 둘 다 보유.
--
-- - owner_id: SAP 인바운드 시 application 이 set 하는 Employee FK.
--   ON DELETE SET NULL — Employee 삭제 시 Account 행은 보존.
-- - owner_sfid: Heroku Connect sync / HerokuMigrationTool 가 채우는 buffer.
--   FK 미부착 (SF User.Id 보존, application 검증 없음).

ALTER TABLE powersales.account
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN owner_sfid VARCHAR(18) NULL;

ALTER TABLE powersales.account
    ADD CONSTRAINT fk_account_owner
    FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
    ON DELETE SET NULL;
