-- =============================================================================
-- branch_mapping 구 조직코드 교정 (강남2지점 ↔ 강남3지점 스왑)
--
-- 배경:
--   branch_mapping.included_branch_codes 는 조직 재코딩 이전의 **구 조직코드**를 함께 담아
--   신/구 코드를 한 지점으로 묶는다 (BranchCodeExpander 가 지점 필터 확장에 사용).
--   출처는 SF Custom Metadata BranchMapping__mdt 이며 Stage1 으로 적재된다.
--
--   그런데 강남2지점 / 강남3지점 2건은 mdt 원본에서 구 코드가 **서로 뒤바뀌어** 있다:
--     5823 강남3 : "5459,5823"  → 5459 는 강남2 의 구 코드
--     5824 강남2 : "5668,5824"  → 5668 은 강남3 의 구 코드
--
--   근거는 같은 SF org 의 sharing rules (구/신 코드를 한 쌍으로 묶은 criteriaItems):
--     sharingRules/MonthlyFemaleEmployeeIntegrationSchedule__c : X5459     → "5459,5824"
--     sharingRules/DisplayWorkScheduleMaster__c                : A54595824 → "5459,5824"
--     (강남3 은 양쪽 모두 "5668,5823")
--   규칙 이름 A54595824 자체가 두 코드를 이어붙인 것이라 쌍이 명시적이다.
--   나머지 30개 지점은 mdt 와 sharing rules 가 일치하므로 교정 대상이 아니다.
--
-- 증상:
--   조직 재코딩 이후 발령이 없어 CostCenterCode__c 가 구 코드로 남은 사원이 지점 필터에서
--   엉뚱한 지점에 계상되거나 누락된다. 실측 — 강남2지점 인원현황이 SF 리포트 36명 대비 신규 35명
--   (구 코드 5459 사원 1명 누락). SF 리포트는 OrgName__c(조직명 문자열) 축이라 영향을 받지 않는다.
--
-- SF 원본(BranchMapping__mdt) 은 변경하지 않는다. 신규 적재분만 교정한다.
--
-- 적재 경로와의 관계:
--   extract-sharing-meta.main.kts 의 BRANCH_MAPPING_OLD_CODE_FIX 가 branch-mapping.csv 생성
--   시점에 동일 교정을 적용하므로, **앞으로 Stage1 을 재적재하면 교정된 값이 들어간다**.
--   본 SQL 은 **이미 적재된 DB** 를 즉시 교정하기 위한 것이다 (Stage1 은 ON CONFLICT DO NOTHING
--   이라 기존 행을 갱신하지 않으므로, 재적재만으로는 기존 행이 고쳐지지 않는다).
--
-- 실행 후 필수:
--   BranchCodeExpander 는 부팅 시 1회 캐시를 빌드하므로 **backend 재기동** 또는 Stage1 적재
--   (Stage1Targets.affectsBranchCodeCache) 로 캐시를 재빌드해야 화면에 반영된다.
--
-- 멱등: WHERE 절이 원본값을 명시하므로 재실행하면 0건 UPDATE.
--
-- 사용법:
--   PGPASSWORD="$(scripts/db-tunnel.sh -s dev --password)" \
--     psql -h localhost -p 15432 -U otkadmin -d otoki \
--     -f scripts/sf-data-migration/fix-branch-mapping-old-codes.sql
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- 변경 전 스냅샷
\echo '--- before ---'
SELECT branch_code, label, included_branch_codes
FROM powersales.branch_mapping
WHERE branch_code IN ('5823', '5824')
ORDER BY branch_code;

-- 강남3지점 — 강남2 구 코드(5459) 제거, 강남3 구 코드(5668) 로 교정
UPDATE powersales.branch_mapping
SET included_branch_codes = '5668,5823'
WHERE branch_code = '5823'
  AND included_branch_codes = '5459,5823';

-- 강남2지점 — 강남3 구 코드(5668) 제거, 강남2 구 코드(5459) 로 교정
UPDATE powersales.branch_mapping
SET included_branch_codes = '5459,5824'
WHERE branch_code = '5824'
  AND included_branch_codes = '5668,5824';

-- 변경 후 확인
\echo '--- after ---'
SELECT branch_code, label, included_branch_codes
FROM powersales.branch_mapping
WHERE branch_code IN ('5823', '5824')
ORDER BY branch_code;

-- 영향 사원 확인 — 구 코드로 남아 각 지점에 새로 포함/제외되는 인원현황 모수
\echo '--- 영향 사원 (구 코드 5459 / 5668 보유, 인원현황 모수 기준) ---'
SELECT e.cost_center_code, e.org_name, e.employee_code, e.name, e.status, e.job_code, e.jikwee
FROM powersales.employee e
WHERE (e.is_deleted IS NULL OR e.is_deleted = false)
  AND e.role IN ('조장', '여사원')
  AND e.job_code IN ('판촉직', '레이디직', 'OSC직')
  AND (e.status IS NULL OR e.status <> '퇴직')
  AND e.name NOT ILIKE '%테스트%'
  AND e.name NOT ILIKE '%관리자%'
  AND e.name NOT ILIKE '%파워세일즈%'
  AND e.cost_center_code IN ('5459', '5668')
ORDER BY e.cost_center_code, e.employee_code;

COMMIT;

\echo ''
\echo '완료. BranchCodeExpander 캐시 재빌드를 위해 backend 재기동이 필요합니다.'
