-- user.employee_code NOT NULL 제약 해제.
--
-- SF 의 일부 User row 는 사번이 NULL — SF 시스템 user (Platform Integration / Process Automated /
-- Data.com Clean 등) + 부서 공용 계정 (KAM1~4영업부 / 영업지원실 / 마케팅실 등). 이들도 신규 시스템에
-- 적재 가능해야 하므로 NOT NULL 제약을 풀고 employee_code 그대로 NULL 로 적재.
--
-- UNIQUE 제약은 유지: PostgreSQL UNIQUE 는 NULL 을 여러 번 허용 (NULL ≠ NULL).
-- 실제 사번 보유 row 들은 여전히 사번 중복 방지됨.

ALTER TABLE powersales."user" ALTER COLUMN employee_code DROP NOT NULL;
