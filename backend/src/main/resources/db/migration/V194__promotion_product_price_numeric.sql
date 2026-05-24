-- promotion_product.price BIGINT → NUMERIC(18, 0)
--
-- 배경:
--   SF describe `DKRetail__Price__c` 는 `double 18/0` (precision 18, scale 0).
--   SF export CSV 가 정수값도 trailing `.0` 으로 직렬화 (예: "8480.0") → Stage1 COPY 가
--   BIGINT 컬럼에 "invalid input syntax for type bigint" 로 실패.
--
-- 해결:
--   컬럼 타입을 SF describe 정합 (NUMERIC(18,0)) 으로 변경. entity 의 price 필드도
--   BigDecimal? 로 함께 변경하여 JPA 자동 변환에 의존하지 않고 명시 타입 정렬.
--
-- 기존 데이터:
--   V159 도입 후 promotion_product 가 운영 데이터 없는 신규 테이블이라 USING 절 안전 (BIGINT → NUMERIC).
--   migration row 가 적재되어 있더라도 BIGINT → NUMERIC(18,0) cast 는 손실 없음.

ALTER TABLE powersales.promotion_product
    ALTER COLUMN price TYPE NUMERIC(18, 0) USING price::NUMERIC(18, 0);
