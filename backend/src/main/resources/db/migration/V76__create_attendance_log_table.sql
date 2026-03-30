-- 출근현황(AttendanceLog) 테이블 생성 (#477)
CREATE TABLE attendance_log (
    attendance_log_id   BIGSERIAL       PRIMARY KEY,
    sfid                VARCHAR(18)     UNIQUE,
    name                VARCHAR(80),
    employee_sfid       VARCHAR(18),
    employee_id         BIGINT,
    attendance_date     TIMESTAMP,
    account_sfid        VARCHAR(18),
    account_id          INTEGER,
    second_work_type    VARCHAR(255),
    reason              VARCHAR(255),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);
