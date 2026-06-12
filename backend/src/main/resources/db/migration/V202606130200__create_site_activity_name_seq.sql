-- DKRetail__SiteAcitivity__c.Name (AutoNumber `SA{00000000}`) 8자리 sequence.
-- 레거시 SF 의 Name AutoNumber 동등. PromotionProduct(`PS{00000000}`) 채번과 동형.
CREATE SEQUENCE IF NOT EXISTS powersales.site_activity_name_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 마이그레이션된 SF 원본 Name (SA00001764 등) 의 최대 suffix 로 sequence 를 추월시켜
-- 신규 채번이 기존 row 와 충돌하지 않도록 1차 보정. SiteActivityNameSeqSyncRunner 가 부팅 시 2차 안전망.
-- 데이터가 있으면 setval(maxSuffix, true) → 다음 nextval 이 maxSuffix+1.
-- 데이터가 없으면(빈 환경) setval(1, false) → 다음 nextval 이 1 (첫 채번 SA00000001, 결번 없음).
SELECT setval(
    'powersales.site_activity_name_seq',
    GREATEST(
        (
            SELECT COALESCE(MAX(SUBSTRING(name FROM 3)::bigint), 0)
            FROM powersales.site_activity
            WHERE name ~ '^SA[0-9]+$'
        ),
        1
    ),
    (SELECT EXISTS (SELECT 1 FROM powersales.site_activity WHERE name ~ '^SA[0-9]+$'))
);
