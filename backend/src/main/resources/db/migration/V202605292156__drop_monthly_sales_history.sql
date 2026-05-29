-- powersales.monthly_sales_history 테이블 폐기.
--
-- 본 테이블의 마감실적 데이터는 ORORA view `ECRM_ABCCUST_MH_V` (entity OroraMonthlySalesHistory) 가
-- 권위 source 로 대체된다. SF formula `MonthlySalesHistory__c.ClosingAmountSum__c` 동등 산출은
-- application 단 derive property (`OroraMonthlySalesHistory.closingAmountSum`) 가 책임.
--
-- 호출 경로:
-- - LastMonthRevenueLookup (DisplayWorkSchedule.lastMonthRevenue 적재) — orora 직전월 row 의
--   abcClosingAmountN+shipClosingAmountN 합으로 SF UpdateLastMonthRevenueBatch.cls 정합 교정 완료.
-- - MfeisThisMonthRevenueBatchService / AdminMonthlyIntegrationService — orora abcClosingAmount1
--   양수 평균으로 마이그레이션 완료.
-- - TeamMemberScheduleSearchService — SF ClosingAmountSum 동등으로 orora 전환 완료.
-- - MonthlySalesService / MonthlySalesAdminQueryService — 100% orora 기반으로 재구현 완료. 목표
--   (thisMonthTarget) / 확정 상태 (isConfirmed) 컬럼은 폐기.
-- - SAP/SF monthly 인바운드 어댑터 (SapSalesHistoryController.upsertMonthly /
--   SfMonthlySalesHistoryController) — 적재 대상 부재로 일괄 삭제 완료.

DROP TABLE IF EXISTS powersales.monthly_sales_history CASCADE;
