-- 스펙 #564: 애플리케이션 DB 사용자(otoki_admin) 의 세션 timezone 을 UTC 로 명시.
--
-- PostgreSQL 의 `now()`, `DEFAULT now()`, `current_timestamp` 등은 세션 timezone 에
-- 따라 wall clock 표현이 달라진다. RDS 기본 설정이 UTC 라 현재 동작은 우연히 UTC 이지만,
-- 운영자가 인스턴스 파라미터를 변경해도 사용자 단위에서 UTC 가 강제되도록 ALTER ROLE 로 박는다.
-- 이미 접속 중인 세션에는 적용되지 않으며, 다음 새 접속부터 효과가 발생한다.

ALTER ROLE otoki_admin SET TIMEZONE = 'UTC';
