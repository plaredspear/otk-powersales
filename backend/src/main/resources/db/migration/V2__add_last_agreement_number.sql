-- F62-P1: GPS 동의 약관 번호 추적 컬럼 추가
ALTER TABLE salesforce2.employee_mng
    ADD COLUMN IF NOT EXISTS last_agreement_number VARCHAR(80);
