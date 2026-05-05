-- Spec #592
-- 주문 요청 번호 (ORD-YYYYMMDD-{seq}) 의 seq 글로벌 시퀀스.
-- order_request_number 는 OrderRequest.orderRequestNumber 가 val 이라 INSERT 시점에 결정 필요.
-- 백엔드 자체 채번이며 SAP 가 채번 안 함 (레거시 SF Auto Number 동등).

CREATE SEQUENCE powersales.order_request_number_seq START WITH 1 INCREMENT BY 1;
