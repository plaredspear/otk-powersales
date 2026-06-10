-- 모바일 앱 패키지(APK/IPA) 버전 관리 테이블.
-- 웹 관리자가 사내 배포용 패키지를 업로드/버전관리하고, 모바일이 최신 버전을 다운로드한다.
-- SF 비대응 자체 엔티티 (BaseEntity 의 created_at/updated_at 만 audit).

CREATE TABLE powersales.app_package (
    app_package_id    BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    platform          VARCHAR(16)  NOT NULL,                       -- ANDROID / IOS
    version_name      VARCHAR(32)  NOT NULL,                       -- 표시용 (예: 1.2.0)
    version_code      BIGINT       NOT NULL,                       -- 정수 비교용 (Android versionCode / iOS buildNumber)
    force_update      BOOLEAN      NOT NULL DEFAULT FALSE,
    release_note      TEXT,
    file_unique_key   VARCHAR(512) NOT NULL,                       -- StorageService uniqueKey (segment 없음)
    file_name         VARCHAR(255) NOT NULL,                       -- 원본 파일명
    file_size         BIGINT       NOT NULL,                       -- bytes
    is_latest         BOOLEAN      NOT NULL DEFAULT FALSE,         -- 최신 지정
    bundle_identifier VARCHAR(255),                                -- iOS plist 용. Android null 허용
    uploaded_by_id    BIGINT,                                      -- 업로더 employee
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uq_app_package_platform_version UNIQUE (platform, version_code),
    CONSTRAINT fk_app_package_uploaded_by FOREIGN KEY (uploaded_by_id)
        REFERENCES powersales.employee (employee_id) ON DELETE SET NULL
);

-- 플랫폼별 최신 버전 1개를 DB 가 보장 (동시성 안전망).
CREATE UNIQUE INDEX uq_app_package_latest_per_platform
    ON powersales.app_package (platform) WHERE is_latest = TRUE;

CREATE INDEX idx_app_package_platform ON powersales.app_package (platform);
