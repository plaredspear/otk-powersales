-- product.product_barcode 컬럼 제거.
--
-- 본 컬럼은 Spec #575 (V17) 에서 SAP 인바운드 ProductBarcode 수신값 보존용으로 추가되었으나:
--   1) SAP 인바운드 적재를 barcode(DKRetail__Barcode__c) 로 전환 — 레거시 IF_REST_SAP_ProductMasterSend
--      이 ProductBarcode → DKRetail__Barcode__c 단일 적재하던 것과 동등.
--   2) 읽기 사용처 0건 (admin 제품 상세는 barcode 컬럼을 노출).
--   3) SF 메타 대응 필드 없음 (@SFField 부재) — SF 데이터 마이그레이션 매핑 대상도 아님.
-- 위 세 사유로 dead column 확정되어 제거한다.
--
-- 같은 V17 에서 추가된 product.pallet 은 계속 사용되므로 보존한다.
-- 별도 테이블 powersales.product_barcode (ProductBarcode 엔티티 / 소비자 바코드) 와는 무관하다.

ALTER TABLE powersales.product
    DROP COLUMN IF EXISTS product_barcode;
