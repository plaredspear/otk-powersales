-- Spec #758 정합 — Appointment.audit FK 대상 Employee → User 전환.
--
-- created_by_id / last_modified_by_id 는 backend `User` entity (SF `User` Object 매핑) 의
-- user_id 를 참조한다. 기존 잔여값은 Employee.employee_id 였으므로 User.user_id 와 의미 정합 깨짐
-- → NULL 초기화 후 SalesforceMigrationTool Phase 2 lookup
-- (`<관계>_sfid` → `user.sfid` → `user.user_id`) 으로 자동 재채움.
--
-- 관련: V92__sf_align_appointment.sql (#736, Employee FK 도입), V113 (AccountCategoryMaster 동일 패턴 선례).

ALTER TABLE powersales.appointment
    DROP CONSTRAINT fk_appointment_created_by,
    DROP CONSTRAINT fk_appointment_last_modified_by;

UPDATE powersales.appointment
    SET created_by_id = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.appointment
    ADD CONSTRAINT fk_appointment_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_appointment_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V92 에서 생성된 것 유지 — 컬럼명 변동 없음.
