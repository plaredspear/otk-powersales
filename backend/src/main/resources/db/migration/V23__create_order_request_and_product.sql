-- Spec #591 P1-B: 본인 주문요청 도메인 활성화
--
-- SF 매핑: DKRetail__OrderRequest__c / DKRetail__OrderRequestProduct__c (DKRetail managed package)
-- HC 매핑: salesforce2.dkretail__orderrequest__c / salesforce2.dkretail__orderrequestproduct__c
-- 본 마이그레이션은 본인 주문요청 목록(#591)/상세(#592)/등록(#595)/취소(#598) 의 공통 인프라.

CREATE TABLE order_request (
    order_request_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_request_number    VARCHAR(80)    NOT NULL UNIQUE,
    employee_id             BIGINT         NOT NULL,
    employee_sfid           VARCHAR(18),
    account_id              BIGINT         NOT NULL,
    account_sfid            VARCHAR(18),
    order_date              TIMESTAMPTZ    NOT NULL,
    delivery_date           DATE           NOT NULL,
    total_amount            NUMERIC(16, 2) NOT NULL,
    total_approved_amount   NUMERIC(16, 2) NOT NULL DEFAULT 0,
    order_request_status    VARCHAR(20)    NOT NULL,
    is_closed               BOOLEAN        NOT NULL DEFAULT FALSE,
    client_deadline_time    VARCHAR(5),
    sfid                    VARCHAR(18),
    created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_request_employee
        FOREIGN KEY (employee_id) REFERENCES employee (employee_id),
    CONSTRAINT fk_order_request_account
        FOREIGN KEY (account_id) REFERENCES account (account_id)
);

CREATE INDEX idx_order_request_employee_id ON order_request (employee_id);
CREATE INDEX idx_order_request_account_id ON order_request (account_id);
CREATE INDEX idx_order_request_order_date ON order_request (order_date);
CREATE INDEX idx_order_request_delivery_date ON order_request (delivery_date);
CREATE INDEX idx_order_request_order_request_status ON order_request (order_request_status);

CREATE TABLE order_request_product (
    order_request_product_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_request_id         BIGINT         NOT NULL,
    order_request_sfid       VARCHAR(18),
    line_number              INTEGER        NOT NULL,
    product_code             VARCHAR(20)    NOT NULL,
    product_name             VARCHAR(100)   NOT NULL,
    quantity_boxes           NUMERIC(16, 2) NOT NULL DEFAULT 0,
    quantity_pieces          INTEGER        NOT NULL DEFAULT 0,
    unit                     VARCHAR(10)    NOT NULL,
    unit_price               NUMERIC(16, 2) NOT NULL DEFAULT 0,
    amount                   NUMERIC(16, 2) NOT NULL DEFAULT 0,
    pieces_per_box           INTEGER        NOT NULL DEFAULT 1,
    min_order_unit           INTEGER        NOT NULL DEFAULT 1,
    supply_quantity          INTEGER        NOT NULL DEFAULT 0,
    dc_quantity              INTEGER        NOT NULL DEFAULT 0,
    is_cancelled             BOOLEAN        NOT NULL DEFAULT FALSE,
    cancelled_at             TIMESTAMP,
    cancelled_by             VARCHAR(8),
    sfid                     VARCHAR(18),
    created_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_request_product_order_request
        FOREIGN KEY (order_request_id) REFERENCES order_request (order_request_id),
    CONSTRAINT idx_order_request_product_unique
        UNIQUE (order_request_id, line_number)
);

CREATE INDEX idx_order_request_product_order_request_id ON order_request_product (order_request_id);
