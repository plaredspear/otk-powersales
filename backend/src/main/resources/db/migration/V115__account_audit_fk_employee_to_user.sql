-- Spec #758 (Account 단독 구현분): Account audit FK 참조 대상 Employee → User 전환.
--
-- 배경: SF sobject 메타의 OwnerId / CreatedById / LastModifiedById referenceTo == User.
-- 기존 backend 는 SF User Id → Employee 사번 우회 매핑이라 sobject 메타와 어긋남.
-- spec #757 (User entity 신설) 완료 후 본 마이그레이션에서 sobject 메타 정합 회복.
--
-- 정책 (사용자 결정):
-- - Q1 (#756 @SFUserRef 어노테이션): 도입 안 함 — sfid 컬럼은 그대로 sync buffer 로 보존
-- - Q2 (#758 R-2 FK 전환): Account 단독 구현 — 기타 35 entity 는 후속 spec #758 본 구현으로 분리
-- - 기존 owner_id / created_by_id / last_modified_by_id 값 reset (spec #758 Q1 사용자 의향 박제)
--   → 마이그레이션 도구 또는 후속 동기화로 재적재
--
-- application 코드 영향 (본 PR 동반):
-- - Account.kt FK 타입 Employee? → User?
-- - AccountUpsertService: employeeByCode 존재 검증 유지 + userByEmployeeNumber 추가 매칭
-- - AccountUpsertMapper: matchedEmployee → matchedUser 시그니처
-- - EmployeeUpsertService: Employee 신규 생성 시 같은 Transaction 으로 User 도 생성 (invariant 보장)

-- 1) 기존 FK 제약 제거
ALTER TABLE powersales.account
    DROP CONSTRAINT IF EXISTS fk_account_owner,
    DROP CONSTRAINT IF EXISTS fk_account_created_by,
    DROP CONSTRAINT IF EXISTS fk_account_last_modified_by;

-- 2) 기존 FK 값 reset (Employee.id → User.id 의미 재매핑 필요. 운영 데이터는 마이그레이션 도구로 재적재 예정)
UPDATE powersales.account
SET owner_id            = NULL,
    created_by_id       = NULL,
    last_modified_by_id = NULL;

-- 3) FK 참조 대상을 powersales."user" (user_id) 로 변경
ALTER TABLE powersales.account
    ADD CONSTRAINT fk_account_owner
        FOREIGN KEY (owner_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_account_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_account_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;
