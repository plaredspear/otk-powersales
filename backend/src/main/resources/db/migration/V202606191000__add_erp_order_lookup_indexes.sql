-- 거래처별 주문(ERP Order) 조회 키 복합/단일 인덱스 신규 도입 — 상세조회 receive timeout 해소 + 목록/적재 가속.
--
-- 배경:
--   거래처별 주문은 SAP 인바운드로 적재된 로컬 테이블(erp_order / erp_order_product)을 직접 read-only 조회한다.
--   두 테이블 모두 기존 인덱스는 FK/소유자 컬럼(account_id, owner_*, *_by_id)과 sfid 뿐이라,
--   실제 조회 키 컬럼에는 인덱스가 전무해 full table scan 이었다. 특히 주문 상세 조회가
--   erp_order_product 풀스캔으로 모바일 dio receiveTimeout(10초)을 초과했다.
--
-- 조치:
--   1) 주문 상세 라인 목록(findBySapOrderNumberOrderByLineNumberAsc):
--      sap_order_number 등호 + line_number 정렬을 한 인덱스로 흡수 → (sap_order_number, line_number) 복합.
--   2) SAP 인바운드 라인 단건조회(findByExternalKey, 라인당 1회 N+1 적재 경로):
--      external_key 단일 인덱스. (코드상 UPSERT 키이나 기존 데이터 중복 위험이 있어 일반 인덱스로 도입,
--      정합성 검증 후 별도 마이그레이션에서 UNIQUE 승격 검토.)
--   3) 모바일 목록조회 핫패스(findClientOrders):
--      account_id =, delivery_request_date = 두 등호로 좁히고 남은 정렬 키 sap_order_number 를 후행에 둬
--      정렬까지 인덱스로 해소 → (account_id, delivery_request_date, sap_order_number) 복합.
--      이로써 기존 단일 idx_erp_order_account_id (V101) 는 선두 컬럼 중복이 되어 드롭한다
--      (account_id 단독 조회 경로는 이 쿼리 외 없음).
--
-- 비고:
--   - findBySapOrderNumber 단건 헤더 조회는 UNIQUE 제약(erp_order_sap_order_number_key)으로 이미 커버.
--   - deleteByErpOrderOrderDateBefore(배치)는 erp_order_id IN(...) 서브쿼리로 idx_erp_order_product_erp_order_id 가 커버.
--   - erp_order(order_date) 범위 삭제는 주1회 hard-delete 배치 전용이라 인덱스 도입 보류.

CREATE INDEX idx_erp_order_product_sap_order_line
    ON powersales.erp_order_product (sap_order_number, line_number);

CREATE INDEX idx_erp_order_product_external_key
    ON powersales.erp_order_product (external_key);

CREATE INDEX idx_erp_order_account_delivery_sap
    ON powersales.erp_order (account_id, delivery_request_date, sap_order_number);

DROP INDEX powersales.idx_erp_order_account_id;
