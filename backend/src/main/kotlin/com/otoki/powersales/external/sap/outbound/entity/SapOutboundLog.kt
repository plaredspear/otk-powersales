package com.otoki.powersales.external.sap.outbound.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName

@DomainName("SAP송신로그")
@Entity
@Table(name = "sap_outbound_log")
class SapOutboundLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sap_outbound_log_id")
    val id: Long = 0,

    @Column(name = "interface_id", nullable = false, length = 20)
    val interfaceId: String,

    @Column(name = "endpoint_path", nullable = false, length = 200)
    val endpointPath: String,

    @Column(name = "request_count", nullable = false)
    val requestCount: Int,

    @Column(name = "http_status")
    val httpStatus: Int? = null,

    @Column(name = "result_code", length = 10)
    val resultCode: String? = null,

    @Column(name = "result_msg", length = 500)
    val resultMsg: String? = null,

    @Column(name = "attempt_count", nullable = false)
    val attemptCount: Int,

    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long,

    @Column(name = "error_detail", columnDefinition = "TEXT")
    val errorDetail: String? = null,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: LocalDateTime,

    @Column(name = "completed_at", nullable = false)
    val completedAt: LocalDateTime
)
