-- Spec #616 — UploadFile SF 누락 비수식 3개 신규 도입 + parent_type 길이 SF 정합 (Q1/Q2 옵션 1).
--
-- 단일 권위: Salesforce Object (`UploadFile__c`)
--
-- 구현 결정 (SF 정합 — 데이터 절단 방지):
--   - upload_kbn: VARCHAR(200) — SF UploadKbn__c 텍스트(200) 정합 (스펙 §6.3 추정 40 → SF 정합 200)
--   - parent_type: 기존 30 → 40 ALTER — SF Object__c 텍스트(40) 정합 (매핑 추가에 따른 절단 방지)

ALTER TABLE powersales.upload_file
    ALTER COLUMN parent_type TYPE varchar(40),
    ADD COLUMN url        varchar(500),
    ADD COLUMN upload_kbn varchar(200),
    ADD COLUMN file_id    varchar(100);
