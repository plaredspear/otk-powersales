-- Spec #606 — Claim SF 누락 컬럼 16개 신규 도입.
--
-- 단일 권위: docs/plan/old_source_260408/salesforce_object/클레임(DKRetail__Claim__c).md
-- 정책 (스펙 §6.3):
--   - DB 컬럼명: SF 한국어 라벨 의미 + snake_case 가독성 우선
--   - Lookup 필드는 <관계명>_sfid (Q2 결정 — product_sfid)
--   - Boolean 은 *_flag 패턴 유지

ALTER TABLE powersales.claim
    ADD COLUMN product_sfid           varchar(18),
    ADD COLUMN customer_delivery_date date,
    ADD COLUMN return_order_number    varchar(100),
    ADD COLUMN expiration_date        date,
    ADD COLUMN interface_date         timestamp without time zone,
    ADD COLUMN manufacturing_date     date,
    ADD COLUMN initial_claim          varchar(250),
    ADD COLUMN logistics_center       varchar(50),
    ADD COLUMN claim_sequence         varchar(255),
    ADD COLUMN detail_sns_name        varchar(250),
    ADD COLUMN cost_center_code       varchar(100),
    ADD COLUMN division               varchar(100),
    ADD COLUMN channel                varchar(20),
    ADD COLUMN sample_collection_flag boolean,
    ADD COLUMN image_count            varchar(10),
    ADD COLUMN action_date            timestamp without time zone;
