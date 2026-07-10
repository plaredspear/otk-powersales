package com.otoki.powersales.external.sap.outbound.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@DomainName("SAP송신로그")
@Entity
@Table(name = "sap_outbound_log")
class SapOutboundLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("SAP송신로그ID")
    @Column(name = "sap_outbound_log_id")
    val id: Long = 0,

    @FieldName("인터페이스ID")
    @Column(name = "interface_id", nullable = false, length = 20)
    val interfaceId: String,

    @FieldName("엔드포인트경로")
    @Column(name = "endpoint_path", nullable = false, length = 200)
    val endpointPath: String,

    @FieldName("요청건수")
    @Column(name = "request_count", nullable = false)
    val requestCount: Int,

    @FieldName("HTTP상태코드")
    @Column(name = "http_status")
    val httpStatus: Int? = null,

    @FieldName("결과코드")
    @Column(name = "result_code", length = 30)
    val resultCode: String? = null,

    @FieldName("결과메시지")
    @Column(name = "result_msg", length = 500)
    val resultMsg: String? = null,

    @FieldName("시도횟수")
    @Column(name = "attempt_count", nullable = false)
    val attemptCount: Int,

    @FieldName("소요시간(ms)")
    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long,

    @FieldName("오류상세")
    @Column(name = "error_detail", columnDefinition = "TEXT")
    val errorDetail: String? = null,

    @FieldName("요청시각")
    @Column(name = "requested_at", nullable = false)
    val requestedAt: LocalDateTime,

    @FieldName("완료시각")
    @Column(name = "completed_at", nullable = false)
    val completedAt: LocalDateTime
)
