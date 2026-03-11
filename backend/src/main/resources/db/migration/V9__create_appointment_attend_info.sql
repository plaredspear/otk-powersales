CREATE TABLE IF NOT EXISTS salesforce2.appointment (
    id              BIGSERIAL PRIMARY KEY,
    employee_code   VARCHAR(20)  NOT NULL,
    emp_code_exist  BOOLEAN      NOT NULL DEFAULT FALSE,
    after_org_code  VARCHAR(20),
    after_org_name  VARCHAR(100),
    jikchak         VARCHAR(50),
    jikwee          VARCHAR(50),
    jikgub          VARCHAR(20),
    work_type       VARCHAR(20),
    manage_type     VARCHAR(50),
    job_code        VARCHAR(20),
    work_area       VARCHAR(50),
    jikjong         VARCHAR(50),
    appoint_date    VARCHAR(8)   NOT NULL,
    job_name        VARCHAR(100),
    ord_detail_code VARCHAR(20),
    ord_detail_node VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_appointment_employee ON salesforce2.appointment (employee_code);
CREATE INDEX IF NOT EXISTS idx_appointment_date ON salesforce2.appointment (appoint_date);

CREATE TABLE IF NOT EXISTS salesforce2.attend_info (
    id              BIGSERIAL PRIMARY KEY,
    employee_code   VARCHAR(20) NOT NULL,
    start_date      VARCHAR(8)  NOT NULL,
    end_date        VARCHAR(8),
    attend_type     VARCHAR(50),
    status          VARCHAR(20),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attend_info_employee ON salesforce2.attend_info (employee_code);
CREATE INDEX IF NOT EXISTS idx_attend_info_start_date ON salesforce2.attend_info (start_date);
