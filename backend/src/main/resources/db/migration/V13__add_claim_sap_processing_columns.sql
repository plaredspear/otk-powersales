-- 스펙 #561: SAP 클레임 상태 인바운드용 컬럼 추가.
-- SAP 가 후속 상태(처리/완료) 로 갱신하는 7개 nullable 컬럼을 claim 테이블에 추가한다.
-- name 은 UPSERT 조회 키 (Salesforce DKRetail__Claim__c.Name 매핑).

ALTER TABLE powersales.claim
    ADD COLUMN name character varying(50),
    ADD COLUMN counsel_number character varying(30),
    ADD COLUMN action_code character varying(20),
    ADD COLUMN action_status character varying(50),
    ADD COLUMN act_content text,
    ADD COLUMN reason_type character varying(20),
    ADD COLUMN cosmos_key character varying(50);

CREATE UNIQUE INDEX idx_claim_name_unique
    ON powersales.claim (name)
    WHERE name IS NOT NULL;
