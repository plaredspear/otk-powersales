-- sf-align-suggestion — SF DKRetail__Proposal__c.CostCenterCode__c 데이터 보존 컬럼 추가.
--
-- 배경: Proposal__c 에는 cost center 계열 필드가 둘 존재한다.
--   - OrgCostCenterCode__c (string/100, 라벨 "조직코스트센터코드")
--     → Org__c 조직트리에서 매칭 변환한 코스트센터코드. 이미 org_cost_center_code 로 적재 중.
--   - CostCenterCode__c    (string/255, 라벨 "조직유형")
--     → 등록 사원 소속 코스트센터코드 원본 (Employee.CostCenterCode__c raw 복사).
--       레거시에서는 물류클레임/클레임 조회의 권한 필터 키(WHERE)로 READ 됐다.
--
-- 신규 시스템은 조회 권한 스코프를 employee_id 본인조회(모바일) + DataScope(admin)로 대체 설계하여
-- 이 필드를 WHERE 에 쓰지 않는다. 다만 운영 데이터 원본 보존을 위해 컬럼을 추가하고 Stage 1 에서 적재한다.
-- 길이는 SF 정합(255)으로 절단 방지.

ALTER TABLE powersales.suggestion
    ADD COLUMN cost_center_code VARCHAR(255);

COMMENT ON COLUMN powersales.suggestion.cost_center_code IS
    'SF CostCenterCode__c (string len=255, 라벨 "조직유형") — 등록 사원 소속 코스트센터코드 원본. 데이터 보존용 (신규 조회 권한 필터엔 미사용).';
