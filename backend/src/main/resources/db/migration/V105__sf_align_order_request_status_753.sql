-- Spec #753 STEP4 — OrderRequest.order_request_status SF picklist 한국어 정합
-- 기존: EnumType.STRING 영문 ('DRAFT'/'SENT'/'APPROVED'/'SEND_FAILED'/'CANCELED')
-- 신규: AttributeConverter 한국어 ('임시저장'/'전송'/'승인완료'/'전송실패'/'주문취소')

-- 1. 컬럼 길이 정합 (SF length=255)
ALTER TABLE powersales.order_request ALTER COLUMN order_request_status TYPE VARCHAR(255);

-- 2. 기존 영문 데이터를 한국어로 마이그레이션
UPDATE powersales.order_request SET order_request_status = '임시저장' WHERE order_request_status = 'DRAFT';
UPDATE powersales.order_request SET order_request_status = '전송' WHERE order_request_status = 'SENT';
UPDATE powersales.order_request SET order_request_status = '승인완료' WHERE order_request_status = 'APPROVED';
UPDATE powersales.order_request SET order_request_status = '전송실패' WHERE order_request_status = 'SEND_FAILED';
UPDATE powersales.order_request SET order_request_status = '주문취소' WHERE order_request_status = 'CANCELED';
