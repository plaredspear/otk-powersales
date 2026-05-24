-- Spec #810 — BranchMapping (cost_center_code 이력 합집합 매핑) 마이그레이션
-- SF Custom Metadata Type BranchMapping__mdt 의 74개 인스턴스를 backend reference 데이터로 이전.
-- 데이터 INSERT 는 BranchMappingSyncRunner (Kotlin SoT + ApplicationRunner) 담당.
CREATE TABLE branch_mapping (
    branch_code VARCHAR(20) PRIMARY KEY,
    included_branch_codes VARCHAR(255) NOT NULL,
    label VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE branch_mapping IS 'SF BranchMapping__mdt 마이그레이션. SoT 는 BranchMappingMatrix Kotlin object, 부팅 시 INSERT-only sync. cost_center_code 이력 합집합 매핑 — 조회 시 현행 코드 → 이력+현행 합집합 확장.';
COMMENT ON COLUMN branch_mapping.branch_code IS '현행 cost_center_code (조회자 본인의 현 시점 코드). SF BranchCode__c 정합.';
COMMENT ON COLUMN branch_mapping.included_branch_codes IS '본 branch_code 가 포함하는 cost_center_code 리스트 (콤마 구분, 이력 + 현행). SF IncludedBranchCode__c 정합.';
