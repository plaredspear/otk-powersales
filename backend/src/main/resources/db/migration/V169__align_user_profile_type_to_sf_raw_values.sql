-- Spec #759/#760 hotfix: user.profile_type 의 backend enum 이름 형식을 SF 운영 raw 값으로 일괄 정정.
--
-- 배경:
--   ProfileType enum 의 `value` 를 backend 이름 (예: SALES_REP) → SF 운영 raw 값 (예: 5.영업사원)
--   으로 전환. DB 에 잔존하는 backend 이름 형식 row 들을 SF 운영 형식으로 UPDATE.
--
-- 매핑:
--   SALES_REP        → 5.영업사원
--   BRANCH_MANAGER   → 4.지점장
--   TEAM_LEADER      → 6.조장
--   STAFF            → 9. Staff
--   SYSTEM_ADMIN     → 시스템 관리자
--   MARKETING        → 8.마케팅
--   SALES_MANAGER    → 3.영업부장
--   BUSINESS_DIRECTOR → 2.사업부장
--   DIVISION_HEAD    → 1.본부장
--
-- 미정의 값 (공장관계자 / 7.영업사원 + 조장 / OLS 등) 은 손대지 않는다 — application 의
-- ProfileType.fromValue 가 STAFF 로 fallback (WARN 로그) 처리한다.

UPDATE "user" SET profile_type = '5.영업사원'    WHERE profile_type = 'SALES_REP';
UPDATE "user" SET profile_type = '4.지점장'      WHERE profile_type = 'BRANCH_MANAGER';
UPDATE "user" SET profile_type = '6.조장'        WHERE profile_type = 'TEAM_LEADER';
UPDATE "user" SET profile_type = '9. Staff'      WHERE profile_type = 'STAFF';
UPDATE "user" SET profile_type = '시스템 관리자' WHERE profile_type = 'SYSTEM_ADMIN';
UPDATE "user" SET profile_type = '8.마케팅'      WHERE profile_type = 'MARKETING';
UPDATE "user" SET profile_type = '3.영업부장'    WHERE profile_type = 'SALES_MANAGER';
UPDATE "user" SET profile_type = '2.사업부장'    WHERE profile_type = 'BUSINESS_DIRECTOR';
UPDATE "user" SET profile_type = '1.본부장'      WHERE profile_type = 'DIVISION_HEAD';
