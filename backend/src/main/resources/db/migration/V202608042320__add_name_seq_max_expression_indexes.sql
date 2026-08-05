-- name 채번 쿼리(MAX(regexp_replace(name, ...))) 전체 스캔 제거용 표현식 부분 인덱스.
--
-- 대상 쿼리 (둘 다 "행사마스터 등록 > 행사사원" 화면의 hot path):
--   1) TeamMemberScheduleRepository.allocateNameSeqBlock / getNextNameSeq  ← 행사사원 일정확정
--   2) PromotionEmployeeRepository.getNextPromotionEmployeeNumberSeq       ← 행사사원 등록
--
--   SELECT setval(seq, GREATEST(nextval(seq),
--       COALESCE((SELECT MAX(NULLIF(regexp_replace(name,'\D','','g'),'')::bigint)
--                   FROM <table> WHERE name ~ '^TS[0-9]+$'), 0) + 1) ...)
--
-- 문제: MAX 대상이 표현식이라 인덱스가 없으면 매 채번마다 테이블 전체를 seq scan 하며
-- 행마다 정규식 매칭 1회 + regexp_replace 1회를 평가한다. team_member_schedule 은
-- 1.76M row / 2.9GB (V207 주석 실측치) 라 채번 1회가 수 초 규모로 튄다. 일정확정은
-- 벌크 채번(allocateNameSeqBlock)으로 이미 "확정당 1회" 까지 줄였지만, 그 1회 자체가 비싸다.
--
-- 해법: 채번 쿼리의 표현식/조건과 **문자 그대로 동일한** 표현식 부분 인덱스를 만든다.
--  - MAX(expr) 는 표현식 인덱스의 backward index scan (ORDER BY expr DESC LIMIT 1) 으로 치환된다.
--  - 부분 인덱스 조건이 쿼리 WHERE 와 동일해야 planner 가 predicate 적용을 증명할 수 있다.
--  - regexp_replace / NULLIF / text→bigint 캐스팅은 모두 IMMUTABLE 이라 인덱스 표현식으로 사용 가능.
--  - name 규칙(SF AutoNumber prefix)에 맞는 row 만 담아 인덱스가 작게 유지된다.
--
-- 주의: CONCURRENTLY 미사용 (Flyway 가 마이그레이션을 트랜잭션으로 감싸 사용 불가 — 기존
-- team_member_schedule 인덱스 마이그레이션 V207 / V202606010234 와 동일한 방식). 생성 동안
-- 해당 테이블 쓰기가 잠기므로 배포 시점의 인덱스 빌드 시간을 감안한다.

CREATE INDEX idx_tms_name_seq_num
    ON powersales.team_member_schedule
       (((NULLIF(regexp_replace(name, '\D', '', 'g'), ''))::bigint))
    WHERE name ~ '^TS[0-9]+$';

CREATE INDEX idx_promotion_employee_name_seq_num
    ON powersales.promotion_employee
       (((NULLIF(regexp_replace(name, '\D', '', 'g'), ''))::bigint))
    WHERE name ~ '^PE[0-9]+$';
