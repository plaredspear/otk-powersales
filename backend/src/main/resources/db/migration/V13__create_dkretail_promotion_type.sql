-- 행사유형 마스터 테이블 (Salesforce DKRetail__PromotionType__c Picklist 대체)
CREATE TABLE dkretail__promotion_type (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    display_order   INTEGER NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_dk_promotion_type_name ON dkretail__promotion_type (name);

-- 초기 시딩 데이터 (Salesforce Picklist 값)
INSERT INTO dkretail__promotion_type (name, display_order) VALUES
    ('시식', 1),
    ('시음', 2),
    ('판촉', 3),
    ('증정', 4),
    ('기타', 5);
