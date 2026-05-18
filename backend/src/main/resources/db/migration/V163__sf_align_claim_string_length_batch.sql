-- SF DKRetail__Claim__c 정합 좁힘 (3건) — entity 가 SF 보다 큰 오버스펙 컬럼 SF 정합으로 좁힘
-- Q10: action_status VARCHAR(50) → VARCHAR(10) (SF length=10)
-- Q11: cosmos_key VARCHAR(50) → VARCHAR(40) (SF length=40)
-- Q12: act_content TEXT → VARCHAR(2000) (SF textarea(2000))
ALTER TABLE claim ALTER COLUMN action_status TYPE VARCHAR(10);
ALTER TABLE claim ALTER COLUMN cosmos_key TYPE VARCHAR(40);
ALTER TABLE claim ALTER COLUMN act_content TYPE VARCHAR(2000);
