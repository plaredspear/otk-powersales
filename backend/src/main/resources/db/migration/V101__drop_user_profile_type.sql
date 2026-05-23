-- Spec #806 — ProfileType destructive 폐기.
-- 모든 권한 / Spring Security ROLE / 데이터 스코프 분기가 spec #805 후 Profile entity 기반.
ALTER TABLE powersales."user" DROP COLUMN IF EXISTS profile_type;
