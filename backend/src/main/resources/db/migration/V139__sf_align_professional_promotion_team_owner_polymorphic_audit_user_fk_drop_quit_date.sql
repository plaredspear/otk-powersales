-- ProfessionalPromotionTeamMaster + ProfessionalPromotionTeamHistory SF Object 정합
-- (sf-meta-diff ProfessionalPromotionTeamMaster__c.md).
--
-- 적용 항목 (master + history 동일 패턴 — history 도 OwnerId polymorphic + audit User FK 동일):
-- (Q1) OwnerId polymorphic R-2 — referenceTo=[Group, User]. 단일 Employee FK → User/Group 분기 + XOR CHECK.
-- (Q2) CreatedById FK 타입 전환 (Employee → User). referenceTo=[User].
-- (Q3) LastModifiedById FK 타입 전환 (Employee → User). referenceTo=[User].
-- (Q4) Formula 컬럼 quit_date 제거 — §6.7 위반. Spec #747 추가분이나 운영 사용처 부재 (FullName__r.DKRetail__EndDate__c = Employee.endDate 직접 조회).
-- (Q5) Picklist enum GENERAL 제거 — SF 메타에 "일반" 옵션 부재. application 측 null 의미 매핑으로 전환 (Employee.professionalPromotionTeam 이미 nullable).
--
-- 패턴 출처: V136 (EmployeeInputCriteriaMaster 동일 변환).

-- =====================================================================
-- (1) master — OwnerId polymorphic + audit User FK 전환
-- =====================================================================

-- 기존 owner / audit FK 제약 + owner_id 컬럼 + idx 제거 (V86 명시 이름)
ALTER TABLE powersales.professional_promotion_team_master
    DROP CONSTRAINT fk_ppt_master_owner,
    DROP CONSTRAINT fk_ppt_master_created_by,
    DROP CONSTRAINT fk_ppt_master_last_modified_by;

DROP INDEX IF EXISTS powersales.idx_ppt_master_owner_id;

ALTER TABLE powersales.professional_promotion_team_master
    DROP COLUMN owner_id;

-- OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.professional_promotion_team_master
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.professional_promotion_team_master
    ADD CONSTRAINT fk_ppt_master_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ppt_master_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 허용)
ALTER TABLE powersales.professional_promotion_team_master
    ADD CONSTRAINT chk_ppt_master_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- audit FK 재연결 (employee → user)
ALTER TABLE powersales.professional_promotion_team_master
    ADD CONSTRAINT fk_ppt_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ppt_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- FK 인덱스 (owner_user_id / owner_group_id 신규. created_by_id / last_modified_by_id 는 V86 에서 이미 생성 — 유지)
CREATE INDEX idx_ppt_master_owner_user_id  ON powersales.professional_promotion_team_master (owner_user_id);
CREATE INDEX idx_ppt_master_owner_group_id ON powersales.professional_promotion_team_master (owner_group_id);

-- (Q4) Formula 컬럼 제거
ALTER TABLE powersales.professional_promotion_team_master
    DROP COLUMN quit_date;

-- =====================================================================
-- (2) history — OwnerId polymorphic + audit User FK 전환
-- =====================================================================

-- 기존 owner / audit FK 제약 + owner_id 컬럼 + idx 제거 (V81 명시 이름)
ALTER TABLE powersales.professional_promotion_team_history
    DROP CONSTRAINT fk_professional_promotion_team_history_owner,
    DROP CONSTRAINT fk_professional_promotion_team_history_created_by,
    DROP CONSTRAINT fk_professional_promotion_team_history_last_modified_by;

DROP INDEX IF EXISTS powersales.idx_professional_promotion_team_history_owner_id;

ALTER TABLE powersales.professional_promotion_team_history
    DROP COLUMN owner_id;

-- OwnerId polymorphic 분기 컬럼 추가
ALTER TABLE powersales.professional_promotion_team_history
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.professional_promotion_team_history
    ADD CONSTRAINT fk_ppt_history_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ppt_history_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK
ALTER TABLE powersales.professional_promotion_team_history
    ADD CONSTRAINT chk_ppt_history_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- audit FK 재연결 (employee → user)
ALTER TABLE powersales.professional_promotion_team_history
    ADD CONSTRAINT fk_ppt_history_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_ppt_history_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- FK 인덱스 (owner_user_id / owner_group_id 신규. created_by_id / last_modified_by_id 는 V81 에서 이미 생성 — 유지)
CREATE INDEX idx_ppt_history_owner_user_id  ON powersales.professional_promotion_team_history (owner_user_id);
CREATE INDEX idx_ppt_history_owner_group_id ON powersales.professional_promotion_team_history (owner_group_id);

-- =====================================================================
-- (3) Picklist enum GENERAL 제거 영향 — 운영 DB 데이터 분포 가정
-- =====================================================================
-- GENERAL enum constant 제거 시 영향 받는 컬럼:
--   - powersales.employee.professional_promotion_team (nullable, ProfessionalPromotionTeamType?)
--   - powersales.monthly_female_employee_integration_schedule.professional_promotion_team (nullable)
--   - powersales.professional_promotion_team_history.old_value (nullable)
--   - powersales.professional_promotion_team_history.new_value (NOT NULL + Kotlin non-null)
--
-- 정책 가정: 운영 DB 에 '일반' 값이 남아 있지 않다 (사용자 확인).
-- 따라서 본 마이그레이션은 데이터 변경을 수행하지 않는다.
-- 운영 분포 확인이 필요한 경우 다음 SELECT 를 사용자 수동 실행 (마이그레이션 자체는 비파괴):
--   SELECT COUNT(*) FROM powersales.employee WHERE professional_promotion_team = '일반';
--   SELECT COUNT(*) FROM powersales.monthly_female_employee_integration_schedule WHERE professional_promotion_team = '일반';
--   SELECT COUNT(*) FROM powersales.professional_promotion_team_history WHERE old_value = '일반' OR new_value = '일반';
