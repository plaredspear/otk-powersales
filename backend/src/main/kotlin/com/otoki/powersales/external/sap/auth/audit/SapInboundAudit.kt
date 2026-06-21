package com.otoki.powersales.external.sap.auth.audit

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@DomainName("SAP수신감사로그")
@Entity
@Table(name = "sap_inbound_audit")
class SapInboundAudit(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("SAP수신감사로그ID")
    @Column(name = "sap_inbound_audit_id")
    val id: Long = 0,

    @FieldName("이벤트유형")
    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: String,

    @FieldName("클라이언트ID")
    @Column(name = "client_id", length = 64)
    val clientId: String? = null,

    @FieldName("엔드포인트")
    @Column(name = "endpoint", length = 255)
    val endpoint: String? = null,

    @FieldName("HTTP메소드")
    @Column(name = "http_method", length = 10)
    val httpMethod: String? = null,

    @FieldName("클라이언트IP")
    @Column(name = "client_ip", nullable = false, length = 45)
    val clientIp: String,

    @FieldName("공개범위")
    @Column(name = "scope", length = 500)
    val scope: String? = null,

    @FieldName("수신건수")
    @Column(name = "received_count")
    val receivedCount: Int? = null,

    @FieldName("이전건수")
    @Column(name = "previous_count")
    val previousCount: Int? = null,

    @FieldName("사유")
    @Column(name = "reason", length = 1000)
    val reason: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
