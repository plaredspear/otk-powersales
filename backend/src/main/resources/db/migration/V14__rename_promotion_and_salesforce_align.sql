-- 0단계: 테이블 RENAME
ALTER TABLE promotion RENAME TO dkretail__promotion__c;

-- 인덱스 RENAME
ALTER INDEX idx_promotion_number RENAME TO idx_dk_promotion_number;
ALTER INDEX idx_promotion_account RENAME TO idx_dk_promotion_account;
ALTER INDEX idx_promotion_dates RENAME TO idx_dk_promotion_dates;
ALTER INDEX idx_promotion_cost_center RENAME TO idx_dk_promotion_cost_center;

-- 1단계: 신규 컬럼 추가
ALTER TABLE dkretail__promotion__c ADD COLUMN promotion_type_id BIGINT;
ALTER TABLE dkretail__promotion__c ADD COLUMN actual_amount BIGINT DEFAULT 0;
ALTER TABLE dkretail__promotion__c ADD COLUMN branch_name VARCHAR(100);
ALTER TABLE dkretail__promotion__c ADD COLUMN category VARCHAR(50);
ALTER TABLE dkretail__promotion__c ADD COLUMN product_type VARCHAR(50);
ALTER TABLE dkretail__promotion__c ADD COLUMN is_closed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE dkretail__promotion__c ADD COLUMN professional_team VARCHAR(100);
ALTER TABLE dkretail__promotion__c ADD COLUMN external_id VARCHAR(50);

-- 2단계: 기존 promotion_type 데이터 마이그레이션
UPDATE dkretail__promotion__c p
SET promotion_type_id = COALESCE(
    (SELECT pt.id FROM dkretail__promotion_type pt WHERE pt.name = p.promotion_type),
    (SELECT pt.id FROM dkretail__promotion_type pt WHERE pt.name = '기타')
)
WHERE p.promotion_type IS NOT NULL;

-- 3단계: FK 제약 조건 + 인덱스
ALTER TABLE dkretail__promotion__c
    ADD CONSTRAINT fk_dk_promotion_type
    FOREIGN KEY (promotion_type_id) REFERENCES dkretail__promotion_type(id);

CREATE INDEX idx_dk_promotion_type_id ON dkretail__promotion__c (promotion_type_id);
CREATE INDEX idx_dk_promotion_external_id ON dkretail__promotion__c (external_id);

-- 4단계: 기존 컬럼 DROP
ALTER TABLE dkretail__promotion__c DROP COLUMN promotion_type;
