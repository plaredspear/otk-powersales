-- 스펙 #737: NewProduct 엔티티 신규 생성 + SF Object 정합 (Group A R-2 + Custom 15 + Picklist 3 + RecordType sfid)

CREATE TABLE powersales.new_product (
    new_product_id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                          VARCHAR(18) UNIQUE,
    name                          VARCHAR(80),
    -- Custom 필드 15개
    customer_survey               DATE,
    initiator                     VARCHAR(255),
    management_type               VARCHAR(255),
    marketability_review_report   DATE,
    product_code_sfid             VARCHAR(18),
    product_code_id               BIGINT,
    product_name                  VARCHAR(100) NOT NULL,
    product_code1                 TEXT,
    purpose                       VARCHAR(255) NOT NULL,
    release_review_report         DATE         NOT NULL,
    "release"                     DATE         NOT NULL,
    status                        VARCHAR(255) NOT NULL,
    firstpropose                  DATE         NOT NULL,
    friday_taste                  DATE         NOT NULL,
    upload_description            VARCHAR(255),
    marketing_team                VARCHAR(255),
    -- Group A
    is_deleted                    BOOLEAN,
    record_type_sfid              VARCHAR(18),
    owner_sfid                    VARCHAR(18),
    owner_id                      BIGINT,
    created_by_sfid               VARCHAR(18),
    created_by_id                 BIGINT,
    last_modified_by_sfid         VARCHAR(18),
    last_modified_by_id           BIGINT,
    -- BaseEntity
    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_new_product_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_new_product_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_new_product_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_new_product_product_code
        FOREIGN KEY (product_code_id) REFERENCES powersales.product (product_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_new_product_owner_id ON powersales.new_product (owner_id);
CREATE INDEX idx_new_product_created_by_id ON powersales.new_product (created_by_id);
CREATE INDEX idx_new_product_last_modified_by_id ON powersales.new_product (last_modified_by_id);
CREATE INDEX idx_new_product_product_code_id ON powersales.new_product (product_code_id);
CREATE INDEX idx_new_product_status ON powersales.new_product (status);
