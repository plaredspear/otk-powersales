-- OrderRequestProduct SF 정합: 마이그레이션 과다 필수 해소.
--
-- SF `DKRetail__OrderRequestProduct__c` 의 아래 6개 필드는 메타 정의(prod/sbx field-meta
-- required=false) + describe 실측(nillable=true) 모두 NULL 허용이다. 그러나 V23 최초 생성
-- 이래 DB 컬럼이 NOT NULL 로 남아 있어, SF 가 NULL 로 둔 row 가 Stage1 적재 시 "필수 필드
-- 누락" 으로 전량 탈락했다 (DKRetail__TotalAmount__c 운영 전건 공란 → 495만 row 적재 0건).
--
-- Stage1Targets.kt 의 nullable=false 제거와 함께 DB 도 NOT NULL drop 하여 SF 정합 복원.
-- DEFAULT 0 절은 그대로 유지 (애플리케이션 신규 INSERT 경로는 기존대로 0 기본값 적용,
-- 마이그레이션 경로만 SF NULL 을 NULL 로 보존).
ALTER TABLE powersales.order_request_product ALTER COLUMN line_number     DROP NOT NULL;
ALTER TABLE powersales.order_request_product ALTER COLUMN product_code    DROP NOT NULL;
ALTER TABLE powersales.order_request_product ALTER COLUMN unit            DROP NOT NULL;
ALTER TABLE powersales.order_request_product ALTER COLUMN quantity_boxes  DROP NOT NULL;
ALTER TABLE powersales.order_request_product ALTER COLUMN quantity_pieces DROP NOT NULL;
ALTER TABLE powersales.order_request_product ALTER COLUMN amount          DROP NOT NULL;
