-- 전문행사조 이력(professional_promotion_team_history) 의 원인 마스터 FK 백필 — 1회성 운영 스크립트
--
-- 배경
--   SF 이력 오브젝트(ProfessionalPromotionTeamHistory__c) 에는 마스터 참조 필드가 없어, 이관된 이력은
--   professional_promotion_team_master_id 가 전부 비어 있다. 신규 시스템이 생성한 이력만 이 FK 를 채운다.
--   전문행사조 마스터의 삭제 / 핵심필드 수정 가드(AdminPPTMasterService) 는 이 FK 로 "사원에 반영된
--   마스터인가" 를 판정하므로, 백필 전에는 이관분 마스터(운영 중 유효 마스터의 대다수) 가 가드를
--   통과해 삭제된다 → 사원 전문행사조만 근거 없이 남는 2026-09 장애가 재발한다.
--
-- 매칭 규칙 (이관 이력 → 마스터)
--   같은 사원 + 같은 전문행사조 + 변경 시점(KST) 이 마스터 기간 안. '카레세일조' 는 표시명 변경 이력이
--   있어 DB 에 '카레행사조' 로 적재된 행이 있으므로 두 문자열을 동일 값으로 취급한다.
--   후보가 2건 이상인 이력은 건드리지 않는다 (오연결 방지 — 어차피 가드는 1건만 있으면 성립).
--
-- 실행
--   ① ~ ③ 을 순서대로 실행. ③ 이 실제 UPDATE 이며 트랜잭션으로 감싼 뒤 ④ 로 확인하고 COMMIT.
--   Flyway 마이그레이션으로 만들지 않는다 — checksum 보호 때문에 이후 정정이 어렵다
--   (CLAUDE.md "Flyway 마이그레이션에 reference data INSERT 금지" 와 같은 취지).

-- ─────────────────────────────────────────────────────────────
-- ① 사전 점검 — 출처별 FK 적재 현황
--    created_in_new = true 인데 master_id_null > 0 이면 신규 적재 경로에 버그가 있다는 뜻 (백필 전에 확인).
-- ─────────────────────────────────────────────────────────────
SELECT (h.sfid IS NULL)                                        AS created_in_new,
       count(*)                                                AS rows,
       count(h.professional_promotion_team_master_id)          AS master_id_filled,
       count(*) - count(h.professional_promotion_team_master_id) AS master_id_null
FROM powersales.professional_promotion_team_history h
GROUP BY 1
ORDER BY 1;

-- ─────────────────────────────────────────────────────────────
-- ② dry-run — 백필 가능 건수 / 다중 매칭(제외 대상) 건수
-- ─────────────────────────────────────────────────────────────
WITH cand AS (
    SELECT h.professional_promotion_team_history_id AS history_id,
           m.professional_promotion_team_master_id  AS master_id
    FROM powersales.professional_promotion_team_history h
    JOIN powersales.professional_promotion_team_master m
      ON m.employee_id = h.employee_id
     AND ( m.team_type = h.new_value
        OR (m.team_type IN ('카레세일조', '카레행사조') AND h.new_value IN ('카레세일조', '카레행사조')) )
     AND (h.changed_at AT TIME ZONE 'Asia/Seoul')::date >= m.start_date
     AND (m.end_date IS NULL OR (h.changed_at AT TIME ZONE 'Asia/Seoul')::date <= m.end_date)
    WHERE h.professional_promotion_team_master_id IS NULL
      AND h.new_value IS NOT NULL
)
SELECT count(*) FILTER (WHERE cnt = 1) AS backfillable,
       count(*) FILTER (WHERE cnt > 1) AS ambiguous_skipped
FROM (SELECT history_id, count(*) AS cnt FROM cand GROUP BY history_id) t;

-- ─────────────────────────────────────────────────────────────
-- ③ 백필 — 단일 매칭 이력만 갱신
-- ─────────────────────────────────────────────────────────────
BEGIN;

WITH cand AS (
    SELECT h.professional_promotion_team_history_id AS history_id,
           m.professional_promotion_team_master_id  AS master_id
    FROM powersales.professional_promotion_team_history h
    JOIN powersales.professional_promotion_team_master m
      ON m.employee_id = h.employee_id
     AND ( m.team_type = h.new_value
        OR (m.team_type IN ('카레세일조', '카레행사조') AND h.new_value IN ('카레세일조', '카레행사조')) )
     AND (h.changed_at AT TIME ZONE 'Asia/Seoul')::date >= m.start_date
     AND (m.end_date IS NULL OR (h.changed_at AT TIME ZONE 'Asia/Seoul')::date <= m.end_date)
    WHERE h.professional_promotion_team_master_id IS NULL
      AND h.new_value IS NOT NULL
),
uniq AS (
    SELECT history_id, min(master_id) AS master_id
    FROM cand
    GROUP BY history_id
    HAVING count(*) = 1
)
UPDATE powersales.professional_promotion_team_history h
   SET professional_promotion_team_master_id = u.master_id,
       updated_at = now()
  FROM uniq u
 WHERE h.professional_promotion_team_history_id = u.history_id;

-- ─────────────────────────────────────────────────────────────
-- ④ 사후 검증 — 현재 유효 마스터 중 몇 건이 가드 대상이 되었는가
--    guarded = 삭제 / 핵심필드 수정이 차단되는 마스터 수.
--    확인 후 COMMIT (문제가 있으면 ROLLBACK).
-- ─────────────────────────────────────────────────────────────
SELECT (m.sfid IS NULL) AS created_in_new,
       count(*)         AS valid_masters,
       count(*) FILTER (
           WHERE EXISTS (SELECT 1
                           FROM powersales.professional_promotion_team_history h
                          WHERE h.professional_promotion_team_master_id = m.professional_promotion_team_master_id)
       )                AS guarded
FROM powersales.professional_promotion_team_master m
WHERE m.is_confirmed
  AND m.start_date <= CURRENT_DATE
  AND (m.end_date IS NULL OR m.end_date >= CURRENT_DATE)
GROUP BY 1
ORDER BY 1;

-- COMMIT;
