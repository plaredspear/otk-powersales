-- upload_file.record_id → record_sfid 컬럼 rename.
--
-- 배경: record_id 는 SF RecordId__c (부모 SObject 의 sfid) 를 보존하는 컬럼이나, 컬럼명이
-- *_sfid 패턴을 따르지 않아 의미가 모호했다. 기존 *_sfid → *_id FK 명명 컨벤션에 맞춰
-- record_sfid 로 변경한다.
--
-- record_sfid 는 parent_type 분기로 claim/notice/proposal/site_activity 를 가리키는 polymorphic
-- 이라 일반 *_sfid → *_id FK substep 대상이 아니다 (SKIP_FK_PREFIXES 에 "record" 등록).
-- parent_id 채움은 SfMigrationStage2Service.runUploadFilePolymorphicParent() 가 (parent_type,
-- record_sfid) → parent_id 로 전용 처리한다.
--
-- idempotent: 컬럼이 이미 rename 된 환경에서도 안전하도록 존재 여부 가드.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'powersales'
          AND table_name = 'upload_file'
          AND column_name = 'record_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'powersales'
          AND table_name = 'upload_file'
          AND column_name = 'record_sfid'
    ) THEN
        ALTER TABLE powersales.upload_file RENAME COLUMN record_id TO record_sfid;
    END IF;
END $$;
