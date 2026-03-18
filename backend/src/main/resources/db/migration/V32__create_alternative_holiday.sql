-- #282 대체휴무 관리 테이블 생성

CREATE TABLE alternative_holiday (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(20) NOT NULL,
    employee_name VARCHAR(50) NOT NULL,
    actual_work_date DATE NOT NULL,
    target_alt_holiday_date DATE NOT NULL,
    confirm_alt_holiday_date DATE,
    status VARCHAR(10) NOT NULL DEFAULT '신규',
    change_reason VARCHAR(500),
    created_by VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alternative_holiday_employee_id ON alternative_holiday (employee_id);
CREATE INDEX idx_alternative_holiday_actual_work_date ON alternative_holiday (actual_work_date);
CREATE INDEX idx_alternative_holiday_status ON alternative_holiday (status);
