-- upload_file.parent_type 값을 SF Object 원형명 → 신규 시스템 엔티티 클래스명으로 교정.
--
-- 설계 확정: parent_type 은 신규 시스템 값(엔티티 클래스명)을 담고, SF 원본 Object__c 는
-- object_type 컬럼에 보존한다. 직전 Stage2 구현이 object_type(SF원형) 을 parent_type 에
-- 그대로 복사해 'DKRetail__Claim__c' 등 SF원형이 잘못 적재된 것을 일괄 교정한다.
--
-- 멱등: 이미 엔티티명인 row 는 매칭 0건.

UPDATE powersales.upload_file SET parent_type = 'Claim'        WHERE parent_type = 'DKRetail__Claim__c';
UPDATE powersales.upload_file SET parent_type = 'Notice'       WHERE parent_type = 'DKRetail__Notice__c';
UPDATE powersales.upload_file SET parent_type = 'Suggestion'   WHERE parent_type = 'DKRetail__Proposal__c';
UPDATE powersales.upload_file SET parent_type = 'SiteActivity' WHERE parent_type = 'DKRetail__SiteAcitivity__c';
