-- V24: 전문행사조 마스터 + 변경 이력 테이블 생성 + Employee 컬럼 추가 (#386-P1)

-- 1. 전문행사조 마스터 테이블
CREATE TABLE salesforce2.professional_promotion_team_master (
    professional_promotion_team_master_id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES salesforce2.employee(employee_id),
    account_id INTEGER NOT NULL REFERENCES salesforce2.account(account_id),
    team_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    branch_code VARCHAR(20),
    branch_name VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pptm_employee_id ON salesforce2.professional_promotion_team_master(employee_id);
CREATE INDEX idx_pptm_account_id ON salesforce2.professional_promotion_team_master(account_id);
CREATE INDEX idx_pptm_team_type ON salesforce2.professional_promotion_team_master(team_type);
CREATE INDEX idx_pptm_start_end_date ON salesforce2.professional_promotion_team_master(start_date, end_date);

-- 2. 전문행사조 변경 이력 테이블
CREATE TABLE salesforce2.professional_promotion_team_history (
    professional_promotion_team_history_id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES salesforce2.employee(employee_id),
    old_value VARCHAR(50),
    new_value VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ppth_employee_id ON salesforce2.professional_promotion_team_history(employee_id);

-- 3. Employee 테이블에 전문행사조 컬럼 추가
ALTER TABLE salesforce2.employee ADD COLUMN professional_promotion_team VARCHAR(50);
