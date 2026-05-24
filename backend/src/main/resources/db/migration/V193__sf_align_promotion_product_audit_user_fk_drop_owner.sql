-- PromotionProduct (DKRetail__PromotionProduct__c) SF Object 정합 보강.
--
-- 배경:
--   V159 으로 promotion_product 테이블 신설 시 promotion/product FK 만 정의되고
--   audit/owner 컬럼이 누락된 비대칭 상태. SF describe 실측 결과:
--     - OwnerId 필드 부재 (Master-Detail child — 부모 Promotion 의 owner 따름)
--     - CreatedById / LastModifiedById 는 User 참조 존재
--
-- 적용 항목:
-- (1) owner_sfid 제거 — SF 측 OwnerId 부재로 sync buffer 의미 없음 (V154 PromotionEmployee 동일 패턴).
-- (2) CreatedById — Lookup(User) → created_by_id (User FK) 신설. created_by_sfid 는 V159 기존.
-- (3) LastModifiedById — Lookup(User) → last_modified_by_id (User FK) 신설. last_modified_by_sfid 는 V159 기존.
--
-- 패턴 출처: V154 (PromotionEmployee owner 제거), V132 (Promotion audit User FK).

BEGIN;

-- (1) owner_sfid 컬럼 제거 — Master-Detail child 라 SF OwnerId 부재 (V154 정합)
ALTER TABLE powersales.promotion_product
    DROP COLUMN IF EXISTS owner_sfid;

-- (2) audit FK id 컬럼 추가
ALTER TABLE powersales.promotion_product
    ADD COLUMN created_by_id       BIGINT,
    ADD COLUMN last_modified_by_id BIGINT;

-- (3) audit FK 제약
ALTER TABLE powersales.promotion_product
    ADD CONSTRAINT fk_promotion_product_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_promotion_product_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- (4) FK 인덱스
CREATE INDEX idx_promotion_product_created_by_id       ON powersales.promotion_product (created_by_id);
CREATE INDEX idx_promotion_product_last_modified_by_id ON powersales.promotion_product (last_modified_by_id);

COMMIT;
