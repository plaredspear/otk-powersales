-- 스펙 #542: SAP 통합 API/인증 제거에 따라 SAP 동기화/송신 로그 테이블 폐기.
-- V1 의 sap_sync_log, V3 의 sap_outbound_log 를 신규 마이그레이션으로 일괄 정리한다.
-- IF EXISTS 로 V3 미적용 fresh DB / V3 적용 DB 모두에서 idempotent.
-- CASCADE 로 외래키/뷰 참조가 있더라도 함께 정리.

DROP TABLE IF EXISTS powersales.sap_sync_log CASCADE;
DROP TABLE IF EXISTS powersales.sap_outbound_log CASCADE;
