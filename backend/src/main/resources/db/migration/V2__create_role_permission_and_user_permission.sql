-- role_permission: 역할별 기본 권한 매핑 (기존 AdminRolePermissions 코드 대체)
CREATE TABLE role_permission (
    role_permission_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role               VARCHAR(20)  NOT NULL,
    permission         VARCHAR(50)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_role_permission UNIQUE (role, permission)
);

-- user_permission: 사용자별 직접 할당 권한
CREATE TABLE user_permission (
    user_permission_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    employee_id        BIGINT       NOT NULL,
    permission         VARCHAR(50)  NOT NULL,
    granted_by         BIGINT       NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_permission UNIQUE (employee_id, permission),
    CONSTRAINT fk_user_permission_employee FOREIGN KEY (employee_id) REFERENCES employee (employee_id),
    CONSTRAINT fk_user_permission_granted_by FOREIGN KEY (granted_by) REFERENCES employee (employee_id)
);

-- 초기 데이터: 기존 AdminRolePermissions 매핑을 그대로 삽입
-- 시스템관리자: 10개 전체
INSERT INTO role_permission (role, permission) VALUES
('시스템관리자', 'DASHBOARD_READ'),
('시스템관리자', 'EMPLOYEE_READ'),
('시스템관리자', 'ACCOUNT_READ'),
('시스템관리자', 'PROMOTION_READ'),
('시스템관리자', 'PROMOTION_WRITE'),
('시스템관리자', 'SAFETY_CHECK_READ'),
('시스템관리자', 'SCHEDULE_READ'),
('시스템관리자', 'SCHEDULE_WRITE'),
('시스템관리자', 'PRODUCT_EXPIRATION_READ'),
('시스템관리자', 'PRODUCT_EXPIRATION_WRITE');

-- 조장: 10개 전체
INSERT INTO role_permission (role, permission) VALUES
('조장', 'DASHBOARD_READ'),
('조장', 'EMPLOYEE_READ'),
('조장', 'ACCOUNT_READ'),
('조장', 'PROMOTION_READ'),
('조장', 'PROMOTION_WRITE'),
('조장', 'SAFETY_CHECK_READ'),
('조장', 'SCHEDULE_READ'),
('조장', 'SCHEDULE_WRITE'),
('조장', 'PRODUCT_EXPIRATION_READ'),
('조장', 'PRODUCT_EXPIRATION_WRITE');

-- 지점장: SCHEDULE_WRITE 제외 9개
INSERT INTO role_permission (role, permission) VALUES
('지점장', 'DASHBOARD_READ'),
('지점장', 'EMPLOYEE_READ'),
('지점장', 'ACCOUNT_READ'),
('지점장', 'PROMOTION_READ'),
('지점장', 'PROMOTION_WRITE'),
('지점장', 'SAFETY_CHECK_READ'),
('지점장', 'SCHEDULE_READ'),
('지점장', 'PRODUCT_EXPIRATION_READ'),
('지점장', 'PRODUCT_EXPIRATION_WRITE');

-- 영업부장: SCHEDULE_WRITE 제외 9개
INSERT INTO role_permission (role, permission) VALUES
('영업부장', 'DASHBOARD_READ'),
('영업부장', 'EMPLOYEE_READ'),
('영업부장', 'ACCOUNT_READ'),
('영업부장', 'PROMOTION_READ'),
('영업부장', 'PROMOTION_WRITE'),
('영업부장', 'SAFETY_CHECK_READ'),
('영업부장', 'SCHEDULE_READ'),
('영업부장', 'PRODUCT_EXPIRATION_READ'),
('영업부장', 'PRODUCT_EXPIRATION_WRITE');

-- 사업부장: SCHEDULE_WRITE 제외 9개
INSERT INTO role_permission (role, permission) VALUES
('사업부장', 'DASHBOARD_READ'),
('사업부장', 'EMPLOYEE_READ'),
('사업부장', 'ACCOUNT_READ'),
('사업부장', 'PROMOTION_READ'),
('사업부장', 'PROMOTION_WRITE'),
('사업부장', 'SAFETY_CHECK_READ'),
('사업부장', 'SCHEDULE_READ'),
('사업부장', 'PRODUCT_EXPIRATION_READ'),
('사업부장', 'PRODUCT_EXPIRATION_WRITE');

-- 영업본부장: SCHEDULE_WRITE 제외 9개
INSERT INTO role_permission (role, permission) VALUES
('영업본부장', 'DASHBOARD_READ'),
('영업본부장', 'EMPLOYEE_READ'),
('영업본부장', 'ACCOUNT_READ'),
('영업본부장', 'PROMOTION_READ'),
('영업본부장', 'PROMOTION_WRITE'),
('영업본부장', 'SAFETY_CHECK_READ'),
('영업본부장', 'SCHEDULE_READ'),
('영업본부장', 'PRODUCT_EXPIRATION_READ'),
('영업본부장', 'PRODUCT_EXPIRATION_WRITE');

-- 영업지원실: 10개 전체
INSERT INTO role_permission (role, permission) VALUES
('영업지원실', 'DASHBOARD_READ'),
('영업지원실', 'EMPLOYEE_READ'),
('영업지원실', 'ACCOUNT_READ'),
('영업지원실', 'PROMOTION_READ'),
('영업지원실', 'PROMOTION_WRITE'),
('영업지원실', 'SAFETY_CHECK_READ'),
('영업지원실', 'SCHEDULE_READ'),
('영업지원실', 'SCHEDULE_WRITE'),
('영업지원실', 'PRODUCT_EXPIRATION_READ'),
('영업지원실', 'PRODUCT_EXPIRATION_WRITE');
