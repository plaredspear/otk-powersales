-- Promotion (DKRetail__Promotion__c) SF picklist 정합 — promotion_type 마스터 테이블 → enum
-- (sf-meta-diff DKRetail__Promotion__c.md Q1 — 사용자 결정 (b) 2026-05-15).
--
-- 적용 항목:
-- - SF picklist `DKRetail__PromotionType__c` (3 옵션 — 시식 / 권장 / 모음전) 을 enum + Converter 로 전환.
-- - 기존 `promotion.promotion_type_id: bigint` (마스터 FK) → `promotion.promotion_type: varchar(255)` 로 백필 + 전환.
-- - 마스터 테이블 `promotion_type` + 시퀀스 제거.
--
-- 패턴 출처: ProductTemperatureType / StandLocation (동일 enum + Converter).

-- (1) promotion.promotion_type VARCHAR(255) 컬럼 추가
ALTER TABLE powersales.promotion
    ADD COLUMN promotion_type VARCHAR(255);

-- (2) 데이터 백필 — promotion_type_id 의 마스터 name 을 promotion_type 컬럼에 채움
UPDATE powersales.promotion p
   SET promotion_type = pt.name
  FROM powersales.promotion_type pt
 WHERE p.promotion_type_id = pt.id;

-- (3) 기존 promotion_type_id 컬럼 제거
ALTER TABLE powersales.promotion
    DROP COLUMN promotion_type_id;

-- (4) promotion_type 마스터 테이블 + 시퀀스 제거
DROP TABLE powersales.promotion_type;
DROP SEQUENCE IF EXISTS powersales.promotion_type_id_seq;
