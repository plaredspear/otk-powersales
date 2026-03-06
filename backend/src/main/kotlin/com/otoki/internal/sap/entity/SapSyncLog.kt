package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sap_sync_log")
class SapSyncLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "api_name", nullable = false, length = 100)
    val apiName: String,

    @Column(name = "request_count", nullable = false)
    val requestCount: Int,

    @Column(name = "success_count", nullable = false)
    val successCount: Int,

    @Column(name = "fail_count", nullable = false)
    val failCount: Int,

    @Column(name = "error_detail", columnDefinition = "TEXT")
    val errorDetail: String? = null,

    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long,

    @Column(name = "request_ip", length = 45)
    val requestIp: String? = null,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: LocalDateTime,

    @Column(name = "completed_at", nullable = false)
    val completedAt: LocalDateTime
)
