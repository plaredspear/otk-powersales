package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HerokuOnly
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 디바이스 버전 관리 Entity (device_version 테이블, 구: device_version_mng)
 *
 * 앱 버전 관리. 복합 PK (version + device)로 최신 버전 조회.
 */
@Entity
@Table(name = "device_version")
@IdClass(DeviceVersionId::class)
@HerokuOnly("device_version_mng")
class DeviceVersion(

    @Id
    @HCColumn("version")
    @Column(name = "version", nullable = false, length = 10)
    val version: String,

    @Id
    @HCColumn("device")
    @Column(name = "device", nullable = false, length = 10)
    val device: String,

    @HCColumn("createdate")
    @Column(name = "create_date", nullable = false)
    val createDate: LocalDateTime,

    @HCColumn("contents")
    @Column(name = "contents", nullable = false, length = 1000)
    val contents: String,

    @HCColumn("s3_key")
    @Column(name = "s3_key", nullable = false, length = 200)
    val s3Key: String,

    @HCColumn("file_url")
    @Column(name = "file_url", length = 300)
    val fileUrl: String? = null,

    @HCColumn("s3_key_ipa")
    @Column(name = "s3_key_ipa", length = 200)
    val s3KeyIpa: String? = null,

    @HCColumn("file_url_ipa")
    @Column(name = "file_url_ipa", length = 300)
    val fileUrlIpa: String? = null
)
