-- V30 에서 추가된 11개 금액 컬럼이 numeric(18, 0) 으로 선언되어 엔티티(Double = float8)와
-- 레거시 salesforce2.monthlysaleshistory__c (모든 금액 컬럼 float8) 컨벤션 모두에서 어긋났음.
-- double precision 으로 정렬한다.

ALTER TABLE powersales.monthly_sales_history
    ALTER COLUMN ship_closing_amount_nh    TYPE double precision USING ship_closing_amount_nh::double precision,
    ALTER COLUMN ship_closing_amount1      TYPE double precision USING ship_closing_amount1::double precision,
    ALTER COLUMN ship_closing_amount2      TYPE double precision USING ship_closing_amount2::double precision,
    ALTER COLUMN ship_closing_amount3      TYPE double precision USING ship_closing_amount3::double precision,
    ALTER COLUMN ship_closing_amount4      TYPE double precision USING ship_closing_amount4::double precision,
    ALTER COLUMN ship_closing_sum_amount   TYPE double precision USING ship_closing_sum_amount::double precision,
    ALTER COLUMN abc_closing_amount4       TYPE double precision USING abc_closing_amount4::double precision,
    ALTER COLUMN abc_closing_sum_amount    TYPE double precision USING abc_closing_sum_amount::double precision,
    ALTER COLUMN last_month_target_by_hand TYPE double precision USING last_month_target_by_hand::double precision,
    ALTER COLUMN this_month_target         TYPE double precision USING this_month_target::double precision;
