package com.otoki.powersales.platform.auth.sharing.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFShareAux
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * sharing recalc endpoint 호출 audit (spec #792).
 *
 * SF mirror 아님 — SF sharing 구현을 보조하는 신규 시스템 자체 운영 audit. `Stage1Targets.ALL` 미등록.
 */
@DomainName("공유재계산로그")
@Entity
@SFShareAux
@Table(name = "sharing_recalc_log")
class SharingRecalcLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("공유재계산로그ID")
    @Column(name = "sharing_recalc_log_id")
    val id: Long = 0,

    @FieldName("실행일시")
    @Column(name = "triggered_at", nullable = false)
    var triggeredAt: OffsetDateTime,

    @FieldName("실행사용자ID")
    @Column(name = "triggered_by_user_id", nullable = false)
    var triggeredByUserId: Long,

    @FieldName("공개범위")
    @Column(name = "scope", nullable = false, length = 50)
    var scope: String,

    @FieldName("SObject명")
    @Column(name = "sobject_name", length = 80)
    var sObjectName: String? = null,

    @FieldName("캐시제거건수")
    @Column(name = "evicted_cache_count", nullable = false)
    var evictedCacheCount: Int = 0,

    @FieldName("소요시간밀리초")
    @Column(name = "duration_ms", nullable = false)
    var durationMs: Int = 0,
) : BaseEntity()
