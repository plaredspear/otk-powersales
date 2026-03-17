-- V28: TeamMemberSchedule.employee_id를 sfid에서 사번으로 전환
-- 1. 컬럼 길이 확장 (18→100, User.employee_id 컬럼과 동일)
ALTER TABLE team_member_schedule ALTER COLUMN employee_id TYPE VARCHAR(100);

-- 2. sfid→사번 변환 (employee 테이블과 JOIN)
-- 매칭되지 않는 sfid(삭제된 사원 등)는 원래 값 유지
UPDATE team_member_schedule tms
SET employee_id = e.employee_id
FROM employee e
WHERE e.sfid = tms.employee_id
  AND tms.employee_id IS NOT NULL;
