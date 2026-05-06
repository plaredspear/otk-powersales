-- Spec #596 — 주문 임시저장 (Draft) API 활성화 보강.
--
-- 1) tmp_order: 사번당 1건 정책 강제 — UNIQUE(employee_id) (Q2 결정).
--    NULL 인 employee_id 는 PostgreSQL UNIQUE 가 다중 NULL 허용하므로 제약 없음.
-- 2) tmp_order_product: #592 정합 컬럼 보강 (Q6, Q7 결정).
--    - tmp_order_id: 헤더 FK (정규화). 백필 후 본 스펙에서는 NULL 허용 유지 (NOT NULL 전환은 후속 스펙).
--    - line_number / unit / quantity / quantity_pieces / quantity_boxes / unit_price / amount.

ALTER TABLE powersales.tmp_order
    ADD CONSTRAINT tmp_order_employee_id_uk UNIQUE (employee_id);

ALTER TABLE powersales.tmp_order_product
    ADD COLUMN tmp_order_id    bigint,
    ADD COLUMN line_number     integer,
    ADD COLUMN unit            varchar(10),
    ADD COLUMN quantity        numeric(16, 2),
    ADD COLUMN quantity_pieces integer,
    ADD COLUMN quantity_boxes  numeric(16, 2),
    ADD COLUMN unit_price      numeric(16, 2),
    ADD COLUMN amount          numeric(16, 2);

-- 백필: 기존 라인의 tmp_order_id 채움 (사번당 1건이라 단순 매칭).
UPDATE powersales.tmp_order_product p
   SET tmp_order_id = o.tmp_order_id
  FROM powersales.tmp_order o
 WHERE p.employee_id IS NOT NULL
   AND p.employee_id = o.employee_id
   AND p.tmp_order_id IS NULL;

CREATE INDEX idx_tmp_order_product_tmp_order_id
    ON powersales.tmp_order_product (tmp_order_id);
