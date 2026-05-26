-- ProfessionalPromotionTeamMaster: FullName__c FK 이중 컬럼 통합 (full_name_id 폐기 → employee_id 단일화).
--
-- 배경:
--   V1 에서 employee_id (신규 시스템 INSERT 경로) 도입.
--   V86 (#728) 에서 SF 정합으로 full_name_sfid + full_name_id 추가 — SF FullName__c lookup 의 별칭 FK.
--   결과적으로 동일한 사원 lookup 이 employee_id / full_name_id 두 컬럼에 분리 적재되며,
--   조회 쿼리 (PPTMasterRepositoryCustomImpl) 가 employee_id 만 JOIN 하므로 SF sync row 의
--   사번/사원명이 화면에 NULL 로 표시되는 버그가 발생.
--
-- 본 마이그레이션:
--   1. 기존 데이터 보존을 위해 full_name_id 값을 employee_id 로 이동 (employee_id IS NULL 인 경우).
--   2. full_name_id FK constraint / index / 컬럼 DROP.
--   3. full_name_sfid 컬럼은 유지 — Stage2 FK resolve 가 full_name_sfid → employee_id 로 직접 매핑 (SfFkResolveTables 의 deriveFkResolveSpec override).

UPDATE powersales.professional_promotion_team_master
SET employee_id = full_name_id
WHERE employee_id IS NULL
  AND full_name_id IS NOT NULL;

ALTER TABLE powersales.professional_promotion_team_master
    DROP CONSTRAINT IF EXISTS fk_ppt_master_full_name;

DROP INDEX IF EXISTS powersales.idx_ppt_master_full_name_id;

ALTER TABLE powersales.professional_promotion_team_master
    DROP COLUMN IF EXISTS full_name_id;
