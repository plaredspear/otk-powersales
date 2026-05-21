-- Spec #780 P1-B: User 테이블에 profile_id / user_role_id FK 컬럼 추가.
--
-- Stage 2 fk substep 이 user.profile_sfid → profile.profile_id / user.user_role_sfid → user_role.user_role_id
-- lookup 으로 자동 채움. 본 컬럼은 read-only audit (Q1 옵션 1 채택 — 둘 다 nullable).

ALTER TABLE powersales."user"
    ADD COLUMN profile_id BIGINT,
    ADD COLUMN user_role_id BIGINT;

ALTER TABLE powersales."user"
    ADD CONSTRAINT fk_user_profile
    FOREIGN KEY (profile_id) REFERENCES powersales.profile (profile_id)
    ON DELETE SET NULL;

ALTER TABLE powersales."user"
    ADD CONSTRAINT fk_user_user_role
    FOREIGN KEY (user_role_id) REFERENCES powersales.user_role (user_role_id)
    ON DELETE SET NULL;

CREATE INDEX idx_user_profile_id ON powersales."user" (profile_id);
CREATE INDEX idx_user_user_role_id ON powersales."user" (user_role_id);
