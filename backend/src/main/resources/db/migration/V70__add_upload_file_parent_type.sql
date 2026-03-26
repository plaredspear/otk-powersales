-- UploadFile 다형성 참조 구분자 추가: parent_type + parent_id
-- Spec #448

-- 1. parent_type 컬럼 추가 (DEFAULT 'UNKNOWN'으로 기존 행 처리)
ALTER TABLE upload_file ADD COLUMN parent_type VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN';

-- 2. parent_id 컬럼 추가 (nullable, 새 시스템 Long PK 참조용)
ALTER TABLE upload_file ADD COLUMN parent_id BIGINT;

-- 3. sfid prefix 기반 parent_type 업데이트
-- a04 prefix → NOTICE (DB JOIN으로 37건 확인)
UPDATE upload_file
SET parent_type = 'NOTICE'
WHERE record_id IS NOT NULL AND LEFT(record_id, 3) = 'a04';

-- 4. NOTICE 행에 대해 notice 테이블 JOIN으로 parent_id 설정
UPDATE upload_file uf
SET parent_id = n.notice_id
FROM notice n
WHERE uf.parent_type = 'NOTICE'
  AND uf.record_id = n.sfid;

-- 5. DEFAULT 제약 제거 (이후 INSERT 시 애플리케이션이 명시적으로 설정)
ALTER TABLE upload_file ALTER COLUMN parent_type DROP DEFAULT;
