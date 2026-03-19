-- Spec #308: employeeNumber(사번) 참조 → employeeId(User PK) 전환
-- 4개 테이블: team_member_schedule, alternative_holiday, safety_check_submission, expirationdate__mng

-- Phase 1: orphan 검증 (실패 시 전체 중단)
DO $$
DECLARE
    orphan_count INT;
BEGIN
    SELECT COUNT(*) INTO orphan_count
    FROM team_member_schedule t
    WHERE t.employee_number IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM users u WHERE u.employee_number = t.employee_number);
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'team_member_schedule: % orphan employee_number records found', orphan_count;
    END IF;

    SELECT COUNT(*) INTO orphan_count
    FROM alternative_holiday t
    WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.employee_number = t.employee_number);
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'alternative_holiday: % orphan employee_number records found', orphan_count;
    END IF;

    SELECT COUNT(*) INTO orphan_count
    FROM safety_check_submission t
    WHERE t.employee_number IS NOT NULL AND t.employee_number != ''
      AND NOT EXISTS (SELECT 1 FROM users u WHERE u.employee_number = t.employee_number);
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'safety_check_submission: % orphan employee_number records found', orphan_count;
    END IF;

    SELECT COUNT(*) INTO orphan_count
    FROM expirationdate__mng t
    WHERE t.employee_number IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM users u WHERE u.employee_number = t.employee_number);
    IF orphan_count > 0 THEN
        RAISE EXCEPTION 'expirationdate__mng: % orphan employee_number records found', orphan_count;
    END IF;
END $$;

-- Phase 2: 일반 테이블 전환 (team_member_schedule, alternative_holiday, expirationdate__mng)

-- team_member_schedule
ALTER TABLE team_member_schedule ADD COLUMN employee_id bigint;
UPDATE team_member_schedule t SET employee_id = u.id FROM users u WHERE u.employee_number = t.employee_number;
ALTER TABLE team_member_schedule ALTER COLUMN employee_id SET NOT NULL;
ALTER TABLE team_member_schedule DROP COLUMN employee_number;

-- alternative_holiday
ALTER TABLE alternative_holiday ADD COLUMN employee_id bigint NOT NULL DEFAULT 0;
UPDATE alternative_holiday t SET employee_id = u.id FROM users u WHERE u.employee_number = t.employee_number;
ALTER TABLE alternative_holiday ALTER COLUMN employee_id DROP DEFAULT;
ALTER TABLE alternative_holiday DROP COLUMN employee_number;

-- expirationdate__mng
ALTER TABLE expirationdate__mng ADD COLUMN employee_id bigint;
UPDATE expirationdate__mng t SET employee_id = u.id FROM users u WHERE u.employee_number = t.employee_number;
ALTER TABLE expirationdate__mng DROP COLUMN employee_number;

-- Phase 3: safety_check_submission 복합PK 전환
ALTER TABLE safety_check_submission DROP CONSTRAINT IF EXISTS safety_check_submission_pkey;
ALTER TABLE safety_check_submission ADD COLUMN employee_id bigint;
UPDATE safety_check_submission t SET employee_id = u.id FROM users u WHERE u.employee_number = t.employee_number;
-- 빈 문자열 employee_number (masterId=""인 레코드)는 employee_id=0 으로 설정
UPDATE safety_check_submission SET employee_id = 0 WHERE employee_id IS NULL AND employee_number = '';
ALTER TABLE safety_check_submission ALTER COLUMN employee_id SET NOT NULL;
ALTER TABLE safety_check_submission DROP COLUMN employee_number;
ALTER TABLE safety_check_submission ADD PRIMARY KEY (master_id, employee_id, working_date);
