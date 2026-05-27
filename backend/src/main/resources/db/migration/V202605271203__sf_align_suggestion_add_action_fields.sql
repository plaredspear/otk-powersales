-- Spec #833 — sf-align-suggestion-action-fields
-- Suggestion (DKRetail__Proposal__c) 의 조치 필드 3종 SF 정합 도입. #664 V173 누락분 보강.
--
-- SF prod describe (DKRetail__Proposal__c, retrieved 2026-05-24):
--   ActionContent__c   textarea len=32768  (조치내용)
--   ActionManager__c   string   len=200    (조치담당자(직급/이름))
--   ActionNum__c       string   len=30     (조치번호)
--
-- 기존 row 영향: 모든 row 의 새 컬럼 = NULL. SF migration stage 가 추후 채움.

ALTER TABLE powersales.suggestion
    ADD COLUMN action_content TEXT,
    ADD COLUMN action_manager VARCHAR(200),
    ADD COLUMN action_num VARCHAR(30);

COMMENT ON COLUMN powersales.suggestion.action_content IS 'SF ActionContent__c (textarea len=32768) — 조치내용';
COMMENT ON COLUMN powersales.suggestion.action_manager IS 'SF ActionManager__c (string len=200) — 조치담당자(직급/이름)';
COMMENT ON COLUMN powersales.suggestion.action_num     IS 'SF ActionNum__c (string len=30, createable=false) — 조치번호 (외부 시스템 발급)';
