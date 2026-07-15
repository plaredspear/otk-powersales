-- 주문 취소 SAP timeout 미확정 라인의 상세조회 시 자동 정합 (#858)
--
-- 주문 취소 SAP 호출(SD03051) 전에 "취소 요청됨" 흔적을 먼저 기록한다.
-- SAP read timeout 등으로 응답을 확정받지 못해 line_change_type='X' 를 세팅하지 못한 라인은
-- 이 흔적을 유지하며, 이후 상세조회(SD03052) 시 SAP DefaultReason 과 교집합으로 line_change_type 을 승격한다.
--
-- cancel_requested_at / cancel_requested_by 는 audit 성격 컬럼 (SF 메타 미존재, @SFField 미부착).
-- cancelled_at / cancelled_by (취소 확정) 와 대칭 — 정합 승격 시 cancel_requested_by 를 cancelled_by 로 승계한다.

ALTER TABLE order_request_product
    ADD COLUMN cancel_requested_at TIMESTAMP,
    ADD COLUMN cancel_requested_by VARCHAR(8);
