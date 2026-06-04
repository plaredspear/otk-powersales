-- DKRetail__Proposal__c SF 정합 — 누락 writeable 필드 `DKRetail__Type__c` (제안유형) 적재 컬럼 추가.
--
-- 단일 권위: Salesforce Object (`DKRetail__Proposal__c`)
-- 필드: DKRetail__Type__c (SF type=string, length=40, 라벨 "제안유형")
-- 매핑: suggestion.type varchar(40) NULL (데이터 보존용 — 신규 조회 로직 미사용)

ALTER TABLE powersales.suggestion
    ADD COLUMN type varchar(40);
