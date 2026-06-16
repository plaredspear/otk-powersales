-- external_api_log 에 요청/응답 본문 컬럼 추가.
-- ExternalApiLogInterceptor 가 local / dev profile 에서만 본문을 캡처해 적재한다 (PII/용량 보호).
-- prod 는 본문을 기록하지 않아 두 컬럼이 NULL 로 남는다.
ALTER TABLE external_api_log ADD COLUMN request_body  TEXT;
ALTER TABLE external_api_log ADD COLUMN response_body TEXT;
