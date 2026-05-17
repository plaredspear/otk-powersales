-- 행사상품 (DKRetail__PromotionProduct__c — DKRetail 관리형 패키지 SObject "상세 POS품목") 신설
-- 레거시 PromotionTriggerHandler 가 행사마스터 신규 등록 / 대표품목 변경 시 자동 생성·upsert
-- 행사 1건당 정확히 1건만 유지 (PromotionIdExt__c 외부 키 동등 — promotion_id UNIQUE 제약)

CREATE TABLE promotion_product (
    promotion_product_id    BIGSERIAL       PRIMARY KEY,
    sfid                    VARCHAR(18),
    name                    VARCHAR(80),

    -- 행사 참조 (Master-Detail 동등 — 행사 1건당 본 레코드 1건만)
    promotion_id            BIGINT          NOT NULL,
    promotion_sfid          VARCHAR(18),

    -- 상품 참조 (Lookup, SetNull — 상품 삭제 시 NULL)
    product_id              BIGINT,
    product_sfid            VARCHAR(18),

    -- SF DKRetail__Price__c (Number(18,0))
    price                   BIGINT,

    -- SF PromotionIdExt__c (외부 키 — 레거시 upsert 키)
    promotion_id_ext        VARCHAR(100),

    -- SF 표준 메타
    owner_sfid              VARCHAR(18),
    created_by_sfid         VARCHAR(18),
    last_modified_by_sfid   VARCHAR(18),
    is_deleted              BOOLEAN         NOT NULL DEFAULT FALSE,

    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_promotion_product_promotion_id UNIQUE (promotion_id),
    CONSTRAINT fk_promotion_product_promotion FOREIGN KEY (promotion_id)
        REFERENCES promotion (promotion_id) ON DELETE CASCADE,
    CONSTRAINT fk_promotion_product_product FOREIGN KEY (product_id)
        REFERENCES product (product_id) ON DELETE SET NULL
);

CREATE INDEX idx_promotion_product_product_id ON promotion_product (product_id);
