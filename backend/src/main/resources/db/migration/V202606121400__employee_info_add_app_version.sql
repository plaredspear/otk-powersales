-- 사용자별 "현재 사용 중인 앱 버전" 스냅샷 컬럼 추가 (employee_info).
-- 로그인/토큰 리프레시 때 클라이언트가 보고한 값으로 덮어쓴다(현재값만 유지, 이력 X).
-- 웹 관리자(사원 상세 > 앱 설정)에서 사용 버전 분포 파악 용도. HC sync 대상 아님(백엔드 전용).
-- app_package(버전 정책 마스터)와 무관 — 이쪽은 각 사용자가 실제 실행 중인 버전.

ALTER TABLE powersales.employee_info
    ADD COLUMN app_version_name    VARCHAR(40),
    ADD COLUMN app_version_code    BIGINT,
    ADD COLUMN app_platform        VARCHAR(20),
    ADD COLUMN app_version_seen_at TIMESTAMPTZ;

COMMENT ON COLUMN powersales.employee_info.app_version_name    IS '사용자가 마지막으로 보고한 앱 버전명(예: 1.0.7)';
COMMENT ON COLUMN powersales.employee_info.app_version_code    IS '사용자가 마지막으로 보고한 앱 빌드번호(versionCode)';
COMMENT ON COLUMN powersales.employee_info.app_platform        IS '앱 플랫폼(ANDROID/IOS)';
COMMENT ON COLUMN powersales.employee_info.app_version_seen_at IS '앱 버전 마지막 보고 시각';
