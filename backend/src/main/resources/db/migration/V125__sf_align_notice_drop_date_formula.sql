-- Notice.notice_date Formula 컬럼 제거 (sf-meta-diff DKRetail__Notice__c.md Q4)
--
-- SF prod 메타: DKRetail__Date__c.calculated = true / calculatedFormula = "LastModifiedDate"
-- §6.7 Formula 컬럼 추가 금지 정책. application 측 BaseEntity.updatedAt.toLocalDate() 로 1:1 재현 가능.
-- V102 가 추가했던 컬럼을 본 마이그레이션에서 drop. 사용처 0건 (grep 검증 완료).

ALTER TABLE powersales.notice DROP COLUMN IF EXISTS notice_date;
