-- Spec #593
-- erp_order.id -> erp_order.erp_order_id
-- erp_order_product.id -> erp_order_product.erp_order_product_id
--
-- PK 컬럼명을 backend-conventions.md 의 {table_name}_id 표준으로 정렬.
-- 기존 sequence 이름(erp_order_id_seq / erp_order_product_id_seq)은 새 컬럼명과 자연스럽게 매칭되어 rename 불필요.

ALTER TABLE powersales.erp_order RENAME COLUMN id TO erp_order_id;
ALTER TABLE powersales.erp_order_product RENAME COLUMN id TO erp_order_product_id;
