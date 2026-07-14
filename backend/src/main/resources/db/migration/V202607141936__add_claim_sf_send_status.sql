-- 제품클레임 SF outbound 전송상태(sfSendStatus) 컬럼 신설 + 기존 전송 lifecycle 컬럼 sf_ prefix 정합.
--
-- 배경: 기존 status(SF DKRetail__Status__c)는 SF → 외부(코스모스) 전송상태이며, SF origin 마이그레이션
-- 데이터에 SF 값(임시저장/전송완료/전송실패)이 그대로 적재된다. 이를 "신규 시스템 → SF 전송상태"의
-- 재전송 판별에 그대로 쓰면 마이그레이션 데이터(예: 전송실패)가 재전송 대상으로 잘못 포함된다.
--
-- 이에 신규→SF 전송상태를 별도 sf_send_status 컬럼으로 분리한다(물류클레임 suggestion.sf_send_status 정합):
--   - sf_send_status : PENDING(전송대기) / SENT(전송완료) / SEND_FAILED(전송실패). SF 매핑 없음.
--   - SF origin 마이그레이션 row 는 sf_send_status=NULL 로 남아 재전송 대상이 아님을 표현한다.
--     신규 등록 건만 등록 시 PENDING 으로 세팅되어 전송 lifecycle 을 탄다.
--
-- 아울러 V202605270133 에서 추가한 전송 lifecycle 컬럼(sent_at / send_fail_message / send_attempt_count)을
-- sf_ prefix 로 정합해 status(코스모스 축) 컬럼과 명확히 구분한다.

ALTER TABLE powersales.claim
    ADD COLUMN sf_send_status VARCHAR(20);

ALTER TABLE powersales.claim
    RENAME COLUMN sent_at TO sf_sent_at;

ALTER TABLE powersales.claim
    RENAME COLUMN send_fail_message TO sf_send_fail_message;

ALTER TABLE powersales.claim
    RENAME COLUMN send_attempt_count TO sf_send_attempt_count;
