package com.otoki.powersales.apppackage.entity

/**
 * 모바일 앱 패키지 배포 플랫폼.
 *
 * - [ANDROID]: APK 파일. presigned URL 직접 다운로드 후 OS 설치 인텐트.
 * - [IOS]: IPA 파일. itms-services manifest.plist OTA 설치 (Enterprise/Ad-hoc 서명 IPA 전제).
 */
enum class AppPlatform {
    ANDROID,
    IOS,
}
