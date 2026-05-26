-- ClaimPhoto 엔티티 제거 — UploadFile 일원화.
-- 클레임 첨부 이미지는 UploadFile (parent_type='DKRetail__Claim__c', parent_id=claim_id) 로 통합 관리.
-- 기존 claim_photos 데이터는 V60__sf_align_claim.sql 에서 이미 DELETE 됨 (운영 잔존 row 없음).

DROP TABLE IF EXISTS powersales.claim_photos;
