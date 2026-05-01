-- Spec #554 P1-B
-- team_member_schedule 테이블에 proxy_registered_by(대리 등록자/조장 employee_id) 컬럼 추가.
-- audit trail 용도. 조장 대리 등록 외 경로(스케줄러 자동 생성 등)에서는 NULL.
ALTER TABLE team_member_schedule
    ADD COLUMN proxy_registered_by BIGINT NULL;
