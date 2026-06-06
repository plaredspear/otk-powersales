-- upload_file 의 parent_type + parent_id 를 record_sfid 직접 조인으로 채운다.
--
-- 배경: SF Object__c (object_type) 는 모바일 등록 경로(claim / site_activity)에서 미설정(NULL)
-- 이라, object_type 기준 parent_type 파생으로는 클레임/현장활동 첨부가 전량 누락된다
-- (운영 실측: object_type 분포에 DKRetail__Claim__c / DKRetail__SiteAcitivity__c 가 0건).
--
-- 해결: record_sfid (부모 SObject Id) 를 각 부모 테이블의 sfid 와 직접 조인해 실제 매칭되는
-- 테이블을 부모로 확정하고 parent_type(엔티티명) + parent_id 를 동시 설정한다. SF Id 는 전역
-- 유니크라 한 record_sfid 는 최대 한 부모 테이블에만 매칭된다.
--
-- 멱등: parent_id IS NULL 한정 — 이미 연결된 row 는 건드리지 않는다.
-- (이미 parent_type 에 SF원형이 잘못 들어간 채 연결된 row 의 교정은 V202606061800 이 담당.)

UPDATE powersales.upload_file uf
SET parent_type = 'Claim', parent_id = c.claim_id
FROM powersales.claim c
WHERE uf.record_sfid = c.sfid AND uf.parent_id IS NULL;

UPDATE powersales.upload_file uf
SET parent_type = 'Notice', parent_id = c.notice_id
FROM powersales.notice c
WHERE uf.record_sfid = c.sfid AND uf.parent_id IS NULL;

UPDATE powersales.upload_file uf
SET parent_type = 'Suggestion', parent_id = c.suggestion_id
FROM powersales.suggestion c
WHERE uf.record_sfid = c.sfid AND uf.parent_id IS NULL;

UPDATE powersales.upload_file uf
SET parent_type = 'SiteActivity', parent_id = c.site_activity_id
FROM powersales.site_activity c
WHERE uf.record_sfid = c.sfid AND uf.parent_id IS NULL;
