package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 디바이스 버전 관리 Entity (device_version_mng 테이블)
 *
 * 앱 버전 관리. 복합 PK (version + device)로 최신 버전 조회.
 */
@Entity
@Table(name = "device_version_mng")
@IdClass(DeviceVersionId::class)
class DeviceVersion(

    @Id
    @Column(name = "version", nullable = false, length = 10)
    val version: String,

    @Id
    @Column(name = "device", nullable = false, length = 10)
    val device: String,

    @Column(name = "createdate", nullable = false)
    val createDate: LocalDateTime,

    @Column(name = "contents", nullable = false, length = 1000)
    val contents: String,

    @Column(name = "s3_key", nullable = false, length = 200)
    val s3Key: String,

    @Column(name = "file_url", length = 300)
    val fileUrl: String? = null,

    @Column(name = "s3_key_ipa", length = 200)
    val s3KeyIpa: String? = null,

    @Column(name = "file_url_ipa", length = 300)
    val fileUrlIpa: String? = null
)
