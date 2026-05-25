-- Spec #694 Q3 — team_member_schedule.promotion_emp_id_ext partial unique index.
-- legacy SF External Id (DKRetail__PromotionEmpIdExt__c) unique 제약 동등 회복.
-- NOT NULL 컬럼 제약은 본 spec 비범위 (backfill 검증 후 후속 spec).

CREATE UNIQUE INDEX IF NOT EXISTS team_member_schedule_promotion_emp_id_ext_unique
    ON team_member_schedule (promotion_emp_id_ext)
    WHERE promotion_emp_id_ext IS NOT NULL;
