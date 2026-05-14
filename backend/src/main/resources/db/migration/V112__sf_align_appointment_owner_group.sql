-- Spec #755: Appointment.OwnerId R-2 정합 + SF Group entity 신설
--
-- SF `Appointment__c.OwnerId.referenceTo = [Group, User]` polymorphic 의 R-2 정합.
-- SF 메타 권위 정책 (sandbox/README.md §6.4 v2.8) — 운영 데이터 분포 (OwnerId 가 User 100%) 와
-- 무관하게 polymorphic 처리 유지.
--
-- (1) "group" 테이블 신설 — SF Group 15 필드 전수 매핑 + Group A R-2 (audit FK → User, Spec #757)
-- (2) appointment 컬럼 3개 추가 — owner_sfid sync buffer + owner_user_id (User FK) + owner_group_id (Group FK)
-- (3) CHECK XOR 제약 chk_appointment_owner_xor — 둘 다 채움 금지, 둘 다 null 허용 (legacy 데이터 보존)
-- (4) FK 인덱스 (PostgreSQL FK 자동 인덱스 미생성)
--
-- "group" 은 PostgreSQL reserved keyword 라 double-quote 필수.

-- ============================================================================
-- (1) "group" 테이블
-- ============================================================================

CREATE TABLE powersales."group" (
    group_id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                          VARCHAR(18),
    name                          VARCHAR(40)  NOT NULL,
    developer_name                VARCHAR(80),
    type                          VARCHAR(40)  NOT NULL,
    related_sfid                  VARCHAR(18),
    owner_sfid                    VARCHAR(18),
    email                         VARCHAR(255),
    does_send_email_to_members    BOOLEAN      NOT NULL DEFAULT false,
    does_include_bosses           BOOLEAN      NOT NULL DEFAULT false,
    description                   TEXT,
    created_by_sfid               VARCHAR(18),
    created_by_id                 BIGINT,
    last_modified_by_sfid         VARCHAR(18),
    last_modified_by_id           BIGINT,
    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_group_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL
);

-- UNIQUE index (sfid 매칭 lookup — sf-migrate Phase 2)
CREATE UNIQUE INDEX idx_group_sfid_unique ON powersales."group" (sfid);

-- FK indexes (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_group_created_by_id        ON powersales."group" (created_by_id);
CREATE INDEX idx_group_last_modified_by_id  ON powersales."group" (last_modified_by_id);

-- ============================================================================
-- (2) appointment 컬럼 추가 — OwnerId R-2 polymorphic
-- ============================================================================

ALTER TABLE powersales.appointment
    ADD COLUMN owner_sfid     VARCHAR(18),
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

ALTER TABLE powersales.appointment
    ADD CONSTRAINT fk_appointment_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_appointment_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 null 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.appointment
    ADD CONSTRAINT chk_appointment_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK indexes
CREATE INDEX idx_appointment_owner_user_id  ON powersales.appointment (owner_user_id);
CREATE INDEX idx_appointment_owner_group_id ON powersales.appointment (owner_group_id);
