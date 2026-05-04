-- 스펙 #583: agreement_history 테이블에 SF 마이그레이션용 sfid 컬럼 3종 추가
--
-- 본 마이그레이션의 목적:
--   1. SalesforceMigrationTool 이 SF AgreementHistory__c → Dev DB agreement_history 로 적재할 때
--      sfid → Long PK 변환 (employee, agreement_word) 의 입력 컬럼을 제공한다.
--   2. 자체 sfid (행 자체의 SF Id) 로 INSERT 멱등성을 보장 (UNIQUE partial index).
--   3. 신규 row 는 모두 NULL — 운영 동의 등록 흐름에는 영향 없음.
--
-- ON DELETE: 컬럼 추가 + partial UNIQUE index 만 추가하며 FK 영향 없음.
-- JPA: ddl-auto = validate. 본 마이그레이션 후 AgreementHistory.kt 에 동일 컬럼 매핑 추가.

ALTER TABLE powersales.agreement_history
    ADD COLUMN sfid character varying(18),
    ADD COLUMN employee_sfid character varying(18),
    ADD COLUMN agreement_word_sfid character varying(18);

-- partial UNIQUE: 신규 row 는 NULL 이므로 NULL 은 unique 제약에서 제외 (V5 패턴 동일)
CREATE UNIQUE INDEX idx_agreement_history_sfid
    ON powersales.agreement_history (sfid)
    WHERE sfid IS NOT NULL;
