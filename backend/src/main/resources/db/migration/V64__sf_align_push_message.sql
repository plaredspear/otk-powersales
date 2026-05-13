-- Spec #709 — PushMessage SF Object 정합 (Group A + Reference R-2)
--
-- 변경 내용:
--   1. owner_sfid / created_by_sfid / last_modified_by_sfid: OwnerId / CreatedById / LastModifiedById sfid buffer
--   2. owner_id / created_by_id / last_modified_by_id: Employee FK (SalesforceMigrationTool 채움)
--   3. employee_id: EmployeeId__c → Employee FK
--
-- 기존 branch / branch_code 컬럼은 V42 에서 이미 추가됨 — 타입 변경 없음 (varchar 그대로, Converter 가 application 측 변환)

ALTER TABLE powersales.push_message
    ADD COLUMN owner_sfid            varchar(18),
    ADD COLUMN owner_id              bigint,
    ADD COLUMN created_by_sfid       varchar(18),
    ADD COLUMN created_by_id         bigint,
    ADD COLUMN last_modified_by_sfid varchar(18),
    ADD COLUMN last_modified_by_id   bigint,
    ADD COLUMN employee_id           bigint;

ALTER TABLE powersales.push_message
    ADD CONSTRAINT fk_push_message_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id),
    ADD CONSTRAINT fk_push_message_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id),
    ADD CONSTRAINT fk_push_message_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id),
    ADD CONSTRAINT fk_push_message_employee
        FOREIGN KEY (employee_id) REFERENCES powersales.employee (employee_id);
