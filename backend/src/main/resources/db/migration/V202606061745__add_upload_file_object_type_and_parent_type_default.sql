-- upload_file.object_type 컬럼 추가 + parent_type DEFAULT 'UNKNOWN' 부여.
--
-- 설계 변경: SF Object__c 를 parent_type 에 직접 적재하던 것을, object_type (SF 원본 보존)
-- 에 적재하고 Stage2 가 object_type 기준으로 parent_type 을 파생하도록 분리한다.
-- (record_sfid → parent_id 와 동일한 "SF 원본 보존 → Stage2 파생" 패턴.)
--
--   - object_type: SF Object__c (부모 SObject API 명) 원본. nullable (SF nillable=true,
--     모바일 등록 경로는 미설정).
--   - parent_type: 신규 시스템 값. Stage1 이 채우지 않아도 DB DEFAULT 'UNKNOWN' 으로 NOT NULL
--     충족. Stage2 의 uploadFilePolymorphicParent substep 이 object_type → parent_type 파생.
--
-- idempotent: 컬럼/디폴트 존재 가드.

-- (1) object_type 컬럼 추가.
ALTER TABLE powersales.upload_file
    ADD COLUMN IF NOT EXISTS object_type varchar(40);

-- (2) 기존 데이터 백필 — parent_type 에 이미 SF 원형이 들어가 있던 환경이면 object_type 으로 복사
--     (테이블이 비어 있으면 0 rows, 무해).
UPDATE powersales.upload_file
SET object_type = parent_type
WHERE object_type IS NULL
  AND parent_type IS NOT NULL
  AND parent_type <> 'UNKNOWN';

-- (3) parent_type DEFAULT 'UNKNOWN' — Stage1 이 parent_type 미지정으로 적재해도 NOT NULL 충족.
ALTER TABLE powersales.upload_file
    ALTER COLUMN parent_type SET DEFAULT 'UNKNOWN';
