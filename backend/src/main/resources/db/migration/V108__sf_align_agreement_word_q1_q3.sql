-- AgreementWord__c ↔ AgreementWord entity 정합 (sf-meta-diff Q1 + Q3 — 2026-05-14)
-- Q1: LastModifiedDate 매핑 정정은 entity 어노테이션 변경만 — 본 SQL 영향 없음.
-- Q3: name / active 컬럼 NOT NULL 제약 추가 (SF nillable=false 정합).

-- NULL backfill (보수적 — 운영 데이터에 NULL 없음을 가정하지만 dev/test 시드 잔재 대비).
UPDATE powersales.agreement_word
SET name = 'AW-' || agreement_word_id
WHERE name IS NULL;

UPDATE powersales.agreement_word
SET active = FALSE
WHERE active IS NULL;

ALTER TABLE powersales.agreement_word ALTER COLUMN name SET NOT NULL;
ALTER TABLE powersales.agreement_word ALTER COLUMN active SET NOT NULL;
