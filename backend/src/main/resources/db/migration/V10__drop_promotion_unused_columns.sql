-- 행사마스터 불필요 필드 제거 (#210)
-- professionalTeam: Salesforce 사원 오브젝트의 필드를 잘못 배치
-- externalId: Salesforce/Heroku 어디에도 없는 필드

DROP INDEX IF EXISTS idx_promotion_external_id;

ALTER TABLE dkretail__promotion__c DROP COLUMN IF EXISTS external_id;
ALTER TABLE dkretail__promotion__c DROP COLUMN IF EXISTS professional_team;
