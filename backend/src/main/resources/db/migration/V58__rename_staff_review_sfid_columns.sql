-- StaffReview employee_id FK 패턴 충돌 해소 + branch_reviews sfid 네이밍 개선 (#430)

-- 1. employee_id: BIGINT → VARCHAR(18) 타입 변경 + employee_sfid로 리네이밍
ALTER TABLE staff_review ALTER COLUMN employee_id TYPE VARCHAR(18) USING employee_id::TEXT;
ALTER TABLE staff_review RENAME COLUMN employee_id TO employee_sfid;

-- 2. branch_reviews → branch_review_sfid 리네이밍
ALTER TABLE staff_review RENAME COLUMN branch_reviews TO branch_review_sfid;
