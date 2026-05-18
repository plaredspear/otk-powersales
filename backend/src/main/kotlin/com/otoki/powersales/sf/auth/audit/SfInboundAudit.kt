package com.otoki.powersales.sf.auth.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "sf_inbound_audit")
class SfInboundAudit(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sf_inbound_audit_id")
    val id: Long = 0,

    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: String,

    @Column(name = "client_id", length = 64)
    val clientId: String? = null,

    @Column(name = "endpoint", length = 255)
    val endpoint: String? = null,

    @Column(name = "http_method", length = 10)
    val httpMethod: String? = null,

    @Column(name = "client_ip", nullable = false, length = 45)
    val clientIp: String,

    @Column(name = "scope", length = 500)
    val scope: String? = null,

    @Column(name = "received_count")
    val receivedCount: Int? = null,

    @Column(name = "reason", length = 1000)
    val reason: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
