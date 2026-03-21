-- Notice: employee_sfid 컬럼 추가 및 employee_id 타입 변환
-- 기존 employee_id(varchar, Salesforce ID)를 employee_sfid로 이관 후 employee_id를 bigint로 변환

-- 1. employee_sfid 컬럼 추가
ALTER TABLE salesforce2.notice ADD COLUMN employee_sfid varchar(18);

-- 2. 기존 employee_id 데이터를 employee_sfid로 복사
UPDATE salesforce2.notice SET employee_sfid = employee_id::varchar WHERE employee_id IS NOT NULL;

-- 3. employee_id 컬럼 DROP
ALTER TABLE salesforce2.notice DROP COLUMN employee_id;

-- 4. employee_id 컬럼을 bigint로 재생성
ALTER TABLE salesforce2.notice ADD COLUMN employee_id bigint;
