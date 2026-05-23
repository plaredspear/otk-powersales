-- Spec #806 — destructive DROP 전 사전 정합 검증.
-- 모든 active user 가 profile_id 보유함을 가정 (spec #805 의 ProfileBootstrapRunner 효과).
-- 가정 위반 시 마이그레이션 실패 + 운영자 수동 정합 의무.
DO $$
DECLARE
    null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count FROM powersales."user" WHERE is_active = TRUE AND profile_id IS NULL;
    IF null_count > 0 THEN
        RAISE EXCEPTION 'spec #806 사전 검증 실패: profile_id IS NULL 인 active user % 명. spec #805 의 ProfileBootstrapRunner 또는 운영자 수동 정합 의무.', null_count;
    END IF;
END $$;
