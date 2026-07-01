-- 공지사항 저장/발행 분리: status 컬럼 추가.
-- DRAFT(임시저장, 모바일 미노출) / PUBLISHED(발행, 모바일 노출).
-- 기존 공지는 이미 노출되던 것이므로 전부 PUBLISHED 로 간주한다.
ALTER TABLE notice ADD COLUMN status varchar(20);
UPDATE notice SET status = 'PUBLISHED' WHERE status IS NULL;
