-- 클레임 관련 테이블 생성

-- 클레임 대분류 카테고리
CREATE TABLE claim_categories (
    id         BIGSERIAL    NOT NULL PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL UNIQUE,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 클레임 세부 카테고리 (종류1 → 종류2)
CREATE TABLE claim_subcategories (
    id          BIGSERIAL    NOT NULL PRIMARY KEY,
    category_id BIGINT       NOT NULL REFERENCES claim_categories(id),
    name        VARCHAR(50)  NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 구매 방법
CREATE TABLE claim_purchase_methods (
    code       VARCHAR(10)  NOT NULL PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 요청사항
CREATE TABLE claim_request_types (
    code       VARCHAR(10)  NOT NULL PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 클레임 본체
CREATE TABLE claims (
    id                   BIGSERIAL      NOT NULL PRIMARY KEY,
    employee_id          BIGINT         NOT NULL REFERENCES employee(employee_id),
    store_id             BIGINT         NOT NULL REFERENCES account(account_id),
    store_name           VARCHAR(100)   NOT NULL,
    product_code         VARCHAR(20)    NOT NULL,
    product_name         VARCHAR(200)   NOT NULL,
    date_type            VARCHAR(20)    NOT NULL,
    date                 DATE           NOT NULL,
    category_id          BIGINT         NOT NULL REFERENCES claim_categories(id),
    subcategory_id       BIGINT         NOT NULL REFERENCES claim_subcategories(id),
    defect_description   VARCHAR(1000)  NOT NULL,
    defect_quantity      INT            NOT NULL,
    purchase_amount      INT,
    purchase_method_code VARCHAR(10),
    purchase_method_name VARCHAR(50),
    request_type_code    VARCHAR(10),
    request_type_name    VARCHAR(50),
    status               VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    created_at           TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_claim_employee_created ON claims (employee_id, created_at);
CREATE INDEX idx_claim_store ON claims (store_id);

-- 클레임 사진
CREATE TABLE claim_photos (
    id                 BIGSERIAL    NOT NULL PRIMARY KEY,
    claim_id           BIGINT       NOT NULL REFERENCES claims(id),
    photo_type         VARCHAR(20)  NOT NULL,
    url                VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size          BIGINT       NOT NULL,
    content_type       VARCHAR(50)  NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT now()
);
