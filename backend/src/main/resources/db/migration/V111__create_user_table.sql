-- Spec #757: SF User Object 매핑 backend entity 신설
--
-- SF 표준 User Object 를 backend `user` entity 로 직접 매핑한다.
-- Employee (인사 마스터) 와 별개로 인증/권한 책임을 분리.
--
-- - Web 로그인 인증 (Mobile 은 Employee.password 별도 운영)
-- - Employee 매칭 키: user.employee_number = employee.employee_code
-- - audit (created_by / last_modified_by / manager) 는 User → User self-reference
-- - "user" 는 PostgreSQL reserved keyword 라 double-quote 필수 (Q5 옵션 1)
--
-- 후속 spec:
-- - #758: R-2 entity audit FK 를 Employee → User 로 일괄 전환
-- - #759: EmployeeProfileResolver + ProfileType enum
-- - #760: Spring Security UserDetailsService (Web 전용)

CREATE TABLE powersales."user" (
    user_id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                          VARCHAR(18),
    username                      VARCHAR(80)  NOT NULL,
    email                         VARCHAR(128),
    is_active                     BOOLEAN      NOT NULL DEFAULT true,
    employee_number               VARCHAR(20)  NOT NULL,
    name                          VARCHAR(121),
    last_name                     VARCHAR(80),
    first_name                    VARCHAR(40),
    alias                         VARCHAR(8),
    title                         VARCHAR(80),
    department                    VARCHAR(80),
    division                      VARCHAR(80),
    mobile_phone                  VARCHAR(40),
    phone                         VARCHAR(40),
    hr_code                       VARCHAR(255),
    branch                        VARCHAR(255),
    last_login_at                 TIMESTAMP WITHOUT TIME ZONE,
    manager_sfid                  VARCHAR(18),
    manager_id                    BIGINT,
    profile_sfid                  VARCHAR(18),
    user_role_sfid                VARCHAR(18),
    password                      VARCHAR(255) NOT NULL,
    password_change_required      BOOLEAN      NOT NULL DEFAULT true,
    created_by_sfid               VARCHAR(18),
    created_by_id                 BIGINT,
    last_modified_by_sfid         VARCHAR(18),
    last_modified_by_id           BIGINT,
    is_deleted                    BOOLEAN,
    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_manager
        FOREIGN KEY (manager_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_user_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_user_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL
);

-- UNIQUE indexes (Q1: Username DB-level unique 보장)
CREATE UNIQUE INDEX idx_user_username_unique         ON powersales."user" (username);
CREATE UNIQUE INDEX idx_user_sfid_unique             ON powersales."user" (sfid);
CREATE UNIQUE INDEX idx_user_employee_number_unique  ON powersales."user" (employee_number);

-- FK indexes (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_user_manager_id            ON powersales."user" (manager_id);
CREATE INDEX idx_user_created_by_id         ON powersales."user" (created_by_id);
CREATE INDEX idx_user_last_modified_by_id   ON powersales."user" (last_modified_by_id);
