package com.otoki.powersales.apppackage.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 모바일 앱 패키지(APK/IPA) 버전.
 *
 * 웹 관리자가 사내 배포용 패키지를 업로드하고, 모바일이 최신 버전을 다운로드한다.
 * SF 비대응 자체 엔티티 (`@SFObject` 없음) — 권한 가드는 SYSTEM(MODIFY_ALL_DATA) 패턴.
 *
 * 플랫폼별 최신 1개는 [isLatest] + 부분 unique index(`uq_app_package_latest_per_platform`)로 DB 가 보장.
 */
@Entity
@Table(name = "app_package")
class AppPackage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_package_id")
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    val platform: AppPlatform,

    /** 표시용 버전 문자열 (예: 1.2.0). */
    @Column(name = "version_name", nullable = false, length = 32)
    var versionName: String,

    /** 정수 비교용 버전 (Android versionCode / iOS buildNumber). */
    @Column(name = "version_code", nullable = false)
    val versionCode: Long,

    /** 강제 업데이트 여부 — 이 버전 미만 클라이언트는 진입 차단 대상. */
    @Column(name = "force_update", nullable = false)
    var forceUpdate: Boolean = false,

    @Column(name = "release_note", columnDefinition = "TEXT")
    var releaseNote: String? = null,

    /** StorageService uniqueKey (segment 없음, 실제 S3 객체는 private/ + key). */
    @Column(name = "file_unique_key", nullable = false, length = 512)
    val fileUniqueKey: String,

    @Column(name = "file_name", nullable = false, length = 255)
    val fileName: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    /** 플랫폼별 최신 지정. 동일 platform 내 단 1개만 true (부분 unique index 보장). */
    @Column(name = "is_latest", nullable = false)
    var isLatest: Boolean = false,

    /** iOS plist 의 bundle-identifier (서명된 IPA 의 실제 값과 일치 필수). Android 는 null. */
    @Column(name = "bundle_identifier", length = 255)
    val bundleIdentifier: String? = null,

    /** 업로더 employee.id. employee 삭제 시 null (ON DELETE SET NULL). */
    @Column(name = "uploaded_by_id")
    val uploadedById: Long? = null,

) : BaseEntity()
