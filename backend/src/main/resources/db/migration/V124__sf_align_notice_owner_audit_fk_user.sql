-- Notice OwnerId + audit FK User 전환 (sf-meta-diff DKRetail__Notice__c.md Q1+Q2+Q3)
--
-- SF prod 메타: OwnerId.referenceTo = [Group, User] polymorphic / CreatedById, LastModifiedById = [User].
-- 레거시 SoT: Apex 전수 grep 결과 Notice 의 OwnerId 에 Group set 코드 0건 → polymorphic 미도입 결정.
-- 결과: 3 FK 모두 employee → user 단순 전환 (V120 BranchReview 와 다른 점: owner 분기 컬럼 미생성).
--
-- (1) 기존 audit / owner FK 제약 제거 (V76 inline FK)
-- (2) FK 제약 재추가 (employee → user)
-- (3) 인덱스는 유지 (V76 의 owner_id / created_by_id / last_modified_by_id 컬럼 그대로)
--
-- 후처리: Phase 2 마이그레이션 도구가 owner_sfid prefix 검증:
--   - '005' (User) → user.sfid lookup → user.user_id FK 채움
--   - '00G' (Group) → skip + 운영 로그 경고 (레거시 실 사용 0건이라 발생 가능성 낮음)

-- (1) 기존 제약 제거
ALTER TABLE powersales.notice
    DROP CONSTRAINT fk_notice_owner,
    DROP CONSTRAINT fk_notice_created_by,
    DROP CONSTRAINT fk_notice_last_modified_by;

-- (2) FK 재연결 (employee → user)
ALTER TABLE powersales.notice
    ADD CONSTRAINT fk_notice_owner
        FOREIGN KEY (owner_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_notice_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_notice_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;
