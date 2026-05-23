package com.otoki.powersales.auth.sharing.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFShareAux
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/**
 * sharing recalc endpoint 호출 audit (spec #792).
 *
 * SF mirror 아님 — SF sharing 구현을 보조하는 신규 시스템 자체 운영 audit. `Stage1Targets.ALL` 미등록.
 */
@Entity
@SFShareAux
@Table(name = "sharing_recalc_log")
class SharingRecalcLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sharing_recalc_log_id")
    val id: Long = 0,

    @Column(name = "triggered_at", nullable = false)
    var triggeredAt: OffsetDateTime,

    @Column(name = "triggered_by_user_id", nullable = false)
    var triggeredByUserId: Long,

    @Column(name = "scope", nullable = false, length = 50)
    var scope: String,

    @Column(name = "sobject_name", length = 80)
    var sObjectName: String? = null,

    @Column(name = "evicted_cache_count", nullable = false)
    var evictedCacheCount: Int = 0,

    @Column(name = "duration_ms", nullable = false)
    var durationMs: Int = 0,
) : BaseEntity()
