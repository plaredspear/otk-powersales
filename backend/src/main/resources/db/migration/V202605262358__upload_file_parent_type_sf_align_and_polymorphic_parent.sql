-- UploadFile.parent_type 정합 + polymorphic parent_id 채움.
--
-- 1) 기존 신규 INSERT (NoticeService / SuggestionService) 가 채워온 단축 코드
--    'NOTICE' / 'SUGGESTION' 를 SF Object 원형으로 변환 — 마이그레이션 row 와 정합.
-- 2) 신규 polymorphic parent_id 채움 — 매핑 표 (UPLOAD_FILE_POLYMORPHIC_PARENTS) 와 동일 정책을
--    SQL 로도 1회 적용해 운영 row 즉시 보강.
--    cut-over 이후 Stage2 polymorphic-parent substep 이 동일 정책을 반복 실행 가능 (멱등).

UPDATE powersales.upload_file
SET parent_type = 'DKRetail__Notice__c'
WHERE parent_type = 'NOTICE';

UPDATE powersales.upload_file
SET parent_type = 'DKRetail__Proposal__c'
WHERE parent_type = 'SUGGESTION';

-- 신규 INSERT 경로는 이미 parent_id 를 함께 채워두므로 본 UPDATE 의 대상은
-- Stage1 마이그레이션 row (parent_type=SF 원형 + parent_id IS NULL) 한정.

UPDATE powersales.upload_file uf
SET parent_id = c.claim_id
FROM powersales.claim c
WHERE uf.parent_type = 'DKRetail__Claim__c'
  AND uf.record_id = c.sfid
  AND uf.parent_id IS NULL;

UPDATE powersales.upload_file uf
SET parent_id = n.notice_id
FROM powersales.notice n
WHERE uf.parent_type = 'DKRetail__Notice__c'
  AND uf.record_id = n.sfid
  AND uf.parent_id IS NULL;

UPDATE powersales.upload_file uf
SET parent_id = s.suggestion_id
FROM powersales.suggestion s
WHERE uf.parent_type = 'DKRetail__Proposal__c'
  AND uf.record_id = s.sfid
  AND uf.parent_id IS NULL;
