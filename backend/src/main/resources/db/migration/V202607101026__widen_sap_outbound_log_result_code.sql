-- sap_outbound_log.result_code 확장 (VARCHAR(10) → VARCHAR(30)).
--
-- 배경: SAP outbound 응답 sink(SapOutboundResponseSink)가 HTML 에러 응답을 감지하면
-- result_code 로 'INVALID_RESPONSE'(16자)를 기록한다. 그러나 컬럼이 VARCHAR(10) 이라
-- INSERT 가 "value too long for type character varying(10)" 로 실패하고, sink 의 try/catch
-- 가 예외를 삼켜 해당 송신 이력 row 가 통째로 유실됐다 (PPT마스터 SD03300 이 HTTP 500 +
-- HTML 에러 페이지를 매번 반환하는 dev 환경에서 재현). 코드 어휘를 절단 없이 담도록
-- 여유를 두어 30 으로 확장한다. 다른 어휘('SUCCESS'/'FAIL'/'NETWORK_ERROR')는 영향 없음.
ALTER TABLE sap_outbound_log
    ALTER COLUMN result_code TYPE VARCHAR(30);
