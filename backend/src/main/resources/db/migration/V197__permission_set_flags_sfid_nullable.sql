-- permission_set_flags.permission_set_sfid NOT NULL → NULL 허용 + partial UNIQUE
--
-- 배경:
--   permission_set_flags 는 XML 메타 출처 (permissionsets/<Name>.permissionset-meta.xml) 이라
--   Stage1 적재 시점에는 permission_set_name 만 알고 sfid 모름. sfid 는 SOQL 출처
--   (PermissionSet entity) 에만 존재. V175 의 NOT NULL UNIQUE 제약이 Stage1 적재를 막음
--   ("null value in column permission_set_sfid violates not-null constraint").
--
-- 해결:
--   1. permission_set_sfid NOT NULL 제약 해제 — Stage1 적재 시점 NULL 허용
--   2. UNIQUE 제약 → partial UNIQUE INDEX (WHERE permission_set_sfid IS NOT NULL) 로 전환 —
--      Stage1 NULL row 다수 허용 + Stage2 fk substep 채움 후 정상 invariant 작동
--
-- Stage2 fk substep 보강:
--   SfMigrationStage2NaturalKeyFkService.resolvePermissionSetFlagsSfid() 전용 method 가
--   permission_set_name → permission_set.name lookup 후 permission_set_sfid 채움.
--   기존 V175 의 (permission_set_sfid → permission_set_flags_id) lookup 은 PSA 의 fk substep 으로
--   유지 (Stage2 fk substep 순서: PSF.permission_set_sfid 채움 → PSF.permission_set_id 채움 →
--   PSA.permission_set_flags_id 채움).

BEGIN;

-- (1) UNIQUE 제약 제거 (V175 의 inline UNIQUE) — PostgreSQL 자동 생성 이름 확인 후 DROP.
-- 자동 생성 이름은 보통 <table>_<col>_key 형태.
ALTER TABLE powersales.permission_set_flags
    DROP CONSTRAINT IF EXISTS permission_set_flags_permission_set_sfid_key;

-- (2) NOT NULL 제약 해제
ALTER TABLE powersales.permission_set_flags
    ALTER COLUMN permission_set_sfid DROP NOT NULL;

-- (3) partial UNIQUE — Stage1 NULL row 다수 허용 + 채움 후 정상 invariant
CREATE UNIQUE INDEX idx_permission_set_flags_permission_set_sfid_unique
    ON powersales.permission_set_flags (permission_set_sfid)
    WHERE permission_set_sfid IS NOT NULL;

COMMIT;
