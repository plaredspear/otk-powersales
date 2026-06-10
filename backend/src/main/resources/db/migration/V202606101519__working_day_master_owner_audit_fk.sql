-- working_day_master — owner/audit FK 관계 컬럼 추가 + working_date_check 타입 정합.
--
-- V202606101000 은 sfid 컬럼(owner_sfid/created_by_sfid/last_modified_by_sfid)만 보유했다.
-- SF 데이터 마이그레이션 Stage 2 fk substep 이 *_sfid → *_id FK 를 information_schema 자동 스캔으로
-- 채우려면 짝이 되는 *_id 컬럼이 필요하므로, 다른 SObject entity(V199/V200 정합)와 동일하게
--   - owner = User/Group polymorphic (owner_user_id + owner_group_id + XOR CHECK)
--   - audit = User FK (created_by_id / last_modified_by_id)
-- 컬럼을 추가한다.
--
-- 아울러 working_date_check 를 INTEGER → DOUBLE PRECISION 으로 변경 (SF describe type = double 정합).

-- (1) owner polymorphic + audit FK 컬럼
ALTER TABLE powersales.working_day_master
    ADD COLUMN owner_user_id        BIGINT,
    ADD COLUMN owner_group_id       BIGINT,
    ADD COLUMN created_by_id        BIGINT,
    ADD COLUMN last_modified_by_id  BIGINT;

ALTER TABLE powersales.working_day_master
    ADD CONSTRAINT fk_working_day_master_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_working_day_master_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT chk_working_day_master_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        ),
    ADD CONSTRAINT fk_working_day_master_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_working_day_master_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

CREATE INDEX idx_working_day_master_owner_user_id       ON powersales.working_day_master (owner_user_id);
CREATE INDEX idx_working_day_master_owner_group_id      ON powersales.working_day_master (owner_group_id);
CREATE INDEX idx_working_day_master_created_by_id       ON powersales.working_day_master (created_by_id);
CREATE INDEX idx_working_day_master_last_modified_by_id ON powersales.working_day_master (last_modified_by_id);

-- (2) working_date_check INTEGER → DOUBLE PRECISION (SF describe type = double 정합)
ALTER TABLE powersales.working_day_master
    ALTER COLUMN working_date_check TYPE DOUBLE PRECISION;
