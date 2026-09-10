-- 전문행사조 이력(professional_promotion_team_history) 의 원인 마스터 FK — 점검 쿼리 모음 (읽기 전용)
--
-- ⚠ 실행(백필)은 이 파일이 아니라 **웹 개발자 도구**에서 합니다:
--     개발자 도구 > 「전문행사조 이력 마스터 연결 백필」  (/admin/tools/ppt-history-master-id-backfill)
--     API: GET  /api/v1/admin/ppt-history/master-id-backfill/preview   (VIEW_ALL_DATA)
--          POST /api/v1/admin/ppt-history/master-id-backfill/execute   (MODIFY_ALL_DATA)
--   매칭 규칙이 두 곳에 중복되면 갈라지므로, UPDATE 는 서버(PPTHistoryRepository) 한 곳에만 둡니다.
--   본 파일은 화면 없이 DB 에서 현황을 직접 확인할 때의 참고용입니다.
--
-- 배경
--   SF 이력 오브젝트(ProfessionalPromotionTeamHistory__c) 에는 마스터 참조 필드가 없어, 이관된 이력은
--   professional_promotion_team_master_id 가 전부 비어 있다. 신규 시스템이 생성한 이력만 이 FK 를 채운다.
--   마스터 삭제 / 핵심필드 수정 가드(AdminPPTMasterService) 는 이 FK 로 "사원에 반영된 마스터인가" 를
--   판정하므로, 백필 전에는 이관분 마스터(운영 중 유효 마스터의 대다수) 가 가드를 통과해 삭제된다
--   → 사원 전문행사조만 근거 없이 남는 2026-09 장애가 재발한다.
--
-- 매칭 규칙 (서버 구현과 동일)
--   같은 사원 + 같은 전문행사조 + 변경 시각(KST) 이 마스터 기간 안.
--   '카레세일조' 는 표시명 변경 이력이 있어 DB 에 '카레행사조' 로 적재된 행이 공존하므로 동일 값으로 취급.
--   후보가 2건 이상인 이력은 오연결 방지를 위해 대상에서 제외한다.

-- ─────────────────────────────────────────────────────────────
-- ① 출처별 FK 적재 현황
--    created_in_new = true 인데 master_id_null > 0 이면 신규 적재 경로에 버그가 있다는 뜻 (백필 전에 확인).
-- ─────────────────────────────────────────────────────────────
SELECT (h.sfid IS NULL)                                          AS created_in_new,
       count(*)                                                  AS rows,
       count(h.professional_promotion_team_master_id)            AS master_id_filled,
       count(*) - count(h.professional_promotion_team_master_id) AS master_id_null
FROM powersales.professional_promotion_team_history h
GROUP BY 1
ORDER BY 1;

-- ─────────────────────────────────────────────────────────────
-- ② 백필 대상 — 화면의 preview 와 동일한 수치
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
       count(*) FILTER (WHERE cnt > 1) AS ambiguous
FROM (SELECT history_id, count(*) AS cnt FROM cand GROUP BY history_id) t;

-- ─────────────────────────────────────────────────────────────
-- ③ 사후 검증 — 현재 유효 마스터 중 몇 건이 가드 대상이 되었는가
--    guarded = 삭제 / 핵심필드 수정이 차단되는 마스터 수. 백필 실행 후 이 값이 valid_masters 에 근접해야 한다.
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
