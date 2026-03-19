-- TeamMemberSchedule sfid 참조 → id(PK) 전환 (#295)

-- 1. teamLeaderSfid → teamLeaderId: User.sfid → User.id 매핑
ALTER TABLE team_member_schedule ADD COLUMN team_leader_id BIGINT;

UPDATE team_member_schedule tms
SET team_leader_id = u.id
FROM employee u
WHERE tms.team_leader_sfid = u.sfid
  AND tms.team_leader_sfid IS NOT NULL;

ALTER TABLE team_member_schedule DROP COLUMN team_leader_sfid;

-- 2. altHolidayId: varchar(18) → bigint (기존 데이터는 정수 문자열)
ALTER TABLE team_member_schedule
    ALTER COLUMN alt_holiday_id TYPE BIGINT
    USING CASE
        WHEN alt_holiday_id ~ '^\d+$' THEN alt_holiday_id::BIGINT
        ELSE NULL
    END;

-- 3. promotionEmpId → promotionEmployeeId: varchar(18) → bigint + 리네이밍
ALTER TABLE team_member_schedule
    ALTER COLUMN promotion_emp_id TYPE BIGINT
    USING CASE
        WHEN promotion_emp_id ~ '^\d+$' THEN promotion_emp_id::BIGINT
        ELSE NULL
    END;

ALTER TABLE team_member_schedule RENAME COLUMN promotion_emp_id TO promotion_employee_id;

-- 4. promotionEmpIdExt 컬럼 삭제 (기존 UNIQUE 인덱스도 함께 삭제됨)
DROP INDEX IF EXISTS uq_tms_promotion_emp_id_ext;
ALTER TABLE team_member_schedule DROP COLUMN promotion_emp_id_ext;

-- 5. promotionEmployeeId에 인덱스 생성
CREATE INDEX idx_tms_promotion_employee_id ON team_member_schedule (promotion_employee_id);
