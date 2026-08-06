-- 물류클레임(제안) 사진의 "SF 로 실제 전송한 key" 를 별도 보관한다.
--
-- 배경: SF `IF_REST_MOBILE_ProposalRegist` 는 이미지 바이트를 받지 않고 UniqueKey 문자열만 `UploadFile__c`
-- 에 저장한 뒤, 렌더 시점에 레거시 공유 버킷 주소에 그 key 를 concat 해 URL 을 만든다
-- (`https://ottogi-nonsap-{dev|prd}-imagerepository-s3.s3.amazonaws.com/` + UniqueKey__c).
-- 신규 시스템은 사진을 파워세일즈 전용 버킷의 `private/` 하위에 저장하므로, upload_file.unique_key 를
-- 그대로 SF 에 보내면 SF 화면에서 이미지가 항상 깨진다(버킷·prefix·ACL 3중 불일치).
--
-- 해결: 등록 시 레거시 공유 버킷에 익명 read 사본을 1장 더 올리고 그 key 를 이 컬럼에 남긴다.
-- 재전송 배치(sf-claim-resend)도 등록 때와 동일한 key 를 보내야 하므로 컬럼으로 영속화한다.
--
-- 기존 row 는 NULL — 전송 경로가 unique_key 로 fallback 하며, 이는 본 컬럼 도입 전과 동일한 동작이다.
ALTER TABLE powersales.upload_file
    ADD COLUMN IF NOT EXISTS sf_unique_key VARCHAR(500);

COMMENT ON COLUMN powersales.upload_file.sf_unique_key IS
    'SF 공유 이미지 저장소 사본의 key. SF UploadFile__c.UniqueKey__c 로 전송하는 값 (unique_key 는 파워세일즈 전용 버킷의 private key 라 SF 가 렌더하지 못함).';
