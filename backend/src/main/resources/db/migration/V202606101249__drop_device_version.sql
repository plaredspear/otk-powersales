-- 레거시 device_version (구 device_version_mng) 테이블 제거.
-- 앱 버전 관리 기능은 app_package(V202606100957)로 신규 재구현되어 device_version 을 대체한다.
-- 레거시 Heroku 버전 row 는 신규로 이관하지 않는다(신규 재업로드 전제). 참조 FK 없음.

DROP TABLE IF EXISTS powersales.device_version;
