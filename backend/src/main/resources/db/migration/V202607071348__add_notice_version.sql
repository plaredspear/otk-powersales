-- 공지사항 낙관적 락(@Version) 컬럼 추가.
-- 여러 관리자가 같은 공지를 동시 편집할 때 발생하는 lost update + 인라인 이미지 교차 오삭제를 차단한다.
-- 기존 row 는 0 으로 초기화. JPA @Version 이 UPDATE 마다 증가시키고, 저장 시 버전 불일치면
-- ObjectOptimisticLockingFailureException(→ 409) 으로 나중 저장자를 거부한다.
ALTER TABLE notice ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
