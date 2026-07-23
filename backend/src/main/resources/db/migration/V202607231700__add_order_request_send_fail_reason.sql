-- 주문 등록 확정 거부 사유 노출용 컬럼 추가
--
-- 비동기 SAP 등록(SD03050) outbox 송신에서 SAP 가 명시적으로 거부(resultCode ≠ 'S')하면 재시도 없이
-- 즉시 SEND_FAILED 로 확정하고, 그 SAP 사유 원문(resutlMsg)을 이 컬럼에 기록한다. 등록 응답은 비동기라
-- 실패 사유를 담을 수 없으므로, 상세 조회 시 사용자에게 "전송실패: {사유}" 로 노출하기 위한 저장소.
--
-- 재전송/재요청으로 다시 SENT 로 복귀하거나 등록이 성공(APPROVED)하면 null 로 초기화된다.
-- 재시도 소진/일시 장애(HTTP_5xx / NETWORK_ERROR 등)로 인한 SEND_FAILED 는 SAP 업무 사유가 아니므로 null.
-- audit/노출 성격 컬럼이라 SF 메타 미존재 (@SFField 미부착).

ALTER TABLE order_request
    ADD COLUMN send_fail_reason TEXT;
