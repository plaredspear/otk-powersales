-- V26: SafetyCheckSubmission 테이블/컬럼명 가독성 개선 (#268)

-- 1. 테이블명 변경
ALTER TABLE safetycheck__workschedule__member RENAME TO safety_check_submission;

-- 2. 컬럼명 변경
ALTER TABLE safety_check_submission RENAME COLUMN "masterId" TO master_id;
ALTER TABLE safety_check_submission RENAME COLUMN employeeid__c TO employee_id;
ALTER TABLE safety_check_submission RENAME COLUMN working__date TO working_date;
ALTER TABLE safety_check_submission RENAME COLUMN starttime TO start_time;
ALTER TABLE safety_check_submission RENAME COLUMN completetime TO complete_time;
ALTER TABLE safety_check_submission RENAME COLUMN yes_chkcnt TO yes_check_count;
ALTER TABLE safety_check_submission RENAME COLUMN no_chkcnt TO no_check_count;
ALTER TABLE safety_check_submission RENAME COLUMN precaution_chkcnt TO precaution_check_count;
ALTER TABLE safety_check_submission RENAME COLUMN traversalflag TO traversal_flag;
ALTER TABLE safety_check_submission RENAME COLUMN eventmasterid TO event_master_id;
ALTER TABLE safety_check_submission RENAME COLUMN completeworkyn TO complete_work_yn;

-- 3. 체크 개수 컬럼 타입 보정 (float8 → int4)
ALTER TABLE safety_check_submission ALTER COLUMN yes_check_count TYPE int4 USING yes_check_count::int4;
ALTER TABLE safety_check_submission ALTER COLUMN no_check_count TYPE int4 USING no_check_count::int4;
ALTER TABLE safety_check_submission ALTER COLUMN precaution_check_count TYPE int4 USING precaution_check_count::int4;

-- 4. 테이블/컬럼 코멘트
COMMENT ON TABLE safety_check_submission IS '안전점검 제출';
COMMENT ON COLUMN safety_check_submission.master_id IS '진열마스터 name (PK)';
COMMENT ON COLUMN safety_check_submission.employee_id IS '사원 ID (PK)';
COMMENT ON COLUMN safety_check_submission.working_date IS '안전점검 일자 (PK)';
COMMENT ON COLUMN safety_check_submission.start_time IS '안전점검 시작일시';
COMMENT ON COLUMN safety_check_submission.complete_time IS '안전점검 완료일시';
COMMENT ON COLUMN safety_check_submission.yes_check_count IS '예 체크 개수';
COMMENT ON COLUMN safety_check_submission.no_check_count IS '해당없음 체크 개수';
COMMENT ON COLUMN safety_check_submission.equipment1 IS '1항목 질문 1';
COMMENT ON COLUMN safety_check_submission.equipment2 IS '1항목 질문 2';
COMMENT ON COLUMN safety_check_submission.equipment3 IS '1항목 질문 3';
COMMENT ON COLUMN safety_check_submission.equipment4 IS '1항목 질문 4';
COMMENT ON COLUMN safety_check_submission.equipment5 IS '1항목 질문 5';
COMMENT ON COLUMN safety_check_submission.equipment6 IS '1항목 질문 6';
COMMENT ON COLUMN safety_check_submission.equipment7 IS '1항목 질문 7';
COMMENT ON COLUMN safety_check_submission.equipment8 IS '1항목 질문 8';
COMMENT ON COLUMN safety_check_submission.equipment9 IS '1항목 질문 9';
COMMENT ON COLUMN safety_check_submission.precaution IS '2항목 질문';
COMMENT ON COLUMN safety_check_submission.precaution_check_count IS '2항목 체크 개수';
COMMENT ON COLUMN safety_check_submission.traversal_flag IS '순회여부 flag';
COMMENT ON COLUMN safety_check_submission.event_master_id IS '행사마스터 sfid';
COMMENT ON COLUMN safety_check_submission.complete_work_yn IS '출근등록 완료 여부';
