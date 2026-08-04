-- name 채번 시퀀스 보정 쿼리(MAX(regexp_replace(name, ...))) 전체 스캔 제거용 표현식 부분 인덱스.
--
-- 대상 쿼리:
--   1) TeamMemberScheduleRepository.syncNameSeq
--   2) PromotionEmployeeRepository.syncNameSeq
--
-- 후속 변경 주의: 본 마이그레이션 작성 시점에는 위 MAX 보정이 **채번 때마다** 실행됐고
-- ("행사마스터 등록 > 행사사원 일정확정" 지연의 주원인), 같은 PR 후속 커밋에서 보정을
-- 부팅 1회 / SF 마이그레이션 직후로 분리했다 (NameSequenceSyncService). 따라서 이 인덱스는
-- 이제 hot path 가 아니라 **보정 시점**을 위한 것이다 — 부팅 때마다 2.9GB 테이블을 전건 스캔하지
-- 않으려면 여전히 필요하다.
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
