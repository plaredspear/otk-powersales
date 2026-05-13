-- 스펙 #738: Employee.postponed_appointment FK 연결 (#713 §4 self-ref 매핑 오류 정정 동반)
--
-- V36 / #713 시점에 employee.postponed_appointment_sfid (VARCHAR 18) 가 sfid 컨벤션으로 추가됨 (FK 보류).
-- 본 마이그레이션에서 postponed_appointment_id BIGINT FK 컬럼 + 제약 + 인덱스 추가.
-- 선행 의존: #736 (Appointment SF 정합 — appointment 테이블의 SF 어노테이션 + sfid + Group A FK 완료)
-- cascade: Appointment 삭제 시 Employee 보존 (FK 만 NULL 처리).

ALTER TABLE powersales.employee
    ADD COLUMN postponed_appointment_id BIGINT;

ALTER TABLE powersales.employee
    ADD CONSTRAINT fk_employee_postponed_appointment
        FOREIGN KEY (postponed_appointment_id) REFERENCES powersales.appointment (appointment_id)
        ON DELETE SET NULL;

CREATE INDEX idx_employee_postponed_appointment_id ON powersales.employee (postponed_appointment_id);
