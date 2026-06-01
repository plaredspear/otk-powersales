-- MonthlySalesHistory 의 SF formula 복제 합계 컬럼 제거.
--
-- 경위: 월별여사원 통합일정 화면의 6개월 평균 마감실적 source 를 ORORA view
-- (OroraMonthlySalesHistory) → RDS MonthlySalesHistory 로 전환하면서, 마감실적 합계는
-- SF formula 복제값 (abc_closing_sum_amount / ship_closing_sum_amount) 대신 개별 카테고리
-- 컬럼을 코드로 재합산 (MonthlySalesHistoryQueryGateway) 하기로 결정.
--
-- 두 컬럼은 SF formula `ABCClosingSumAmount__c` / `ShipClosingSumAmount__c` 의 복제값으로,
-- formula 복제값이 누락/stale 이어도 산출 결과에 영향을 주지 않도록 entity 필드 + SF migration
-- 매핑 (Stage1Targets) 과 함께 완전 제거. 추후 운영 요건으로 다시 필요해지면 SF 무관 일반
-- 컬럼으로 재도입.

ALTER TABLE powersales.monthly_sales_history DROP COLUMN IF EXISTS abc_closing_sum_amount;
ALTER TABLE powersales.monthly_sales_history DROP COLUMN IF EXISTS ship_closing_sum_amount;
