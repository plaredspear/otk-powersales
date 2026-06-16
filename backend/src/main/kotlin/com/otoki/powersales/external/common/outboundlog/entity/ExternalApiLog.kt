package com.otoki.powersales.external.common.outboundlog.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 외부 시스템으로 나가는 모든 HTTP outbound 호출의 공통 관측 로그.
 *
 * `ExternalApiLogInterceptor` ([ClientHttpRequestInterceptor]) 가 SAP / SF / Naver 의
 * `RestClient` builder 에 공통 등록되어, 각 outbound HTTP 호출 1건을 자동으로 1 row 적재한다.
 * sender 코드를 수정하지 않고 cross-cutting 으로 포착하므로, 신규 외부 연동도 같은 builder 를
 * 쓰면 자동으로 기록 대상에 포함된다.
 *
 * SAP 전용 `sap_outbound_log` (interface_id / request_count / 재시도 도메인 의미 보유) 와는 별개로,
 * "모든 외부 HTTP 호출을 한 곳에서 관측" 하는 운영 로그 역할을 담당한다.
 */
@Entity
@Table(name = "external_api_log")
class ExternalApiLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_api_log_id")
    val id: Long = 0,

    /** 호출 대상 외부 시스템 분류 (SAP / SF / NAVER). */
    @Column(name = "target_system", nullable = false, length = 20)
    val targetSystem: String,

    @Column(name = "http_method", nullable = false, length = 10)
    val httpMethod: String,

    /** 호출 URI (query string 포함, 최대 1000자 절단). */
    @Column(name = "uri", nullable = false, length = 1000)
    val uri: String,

    /** HTTP 응답 상태 코드. 네트워크/연결 실패로 응답이 없으면 null. */
    @Column(name = "http_status")
    val httpStatus: Int? = null,

    /** 호출 성공 여부 — 2xx 응답이면 true, 그 외(4xx/5xx/네트워크 예외)면 false. */
    @Column(name = "success", nullable = false)
    val success: Boolean,

    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long,

    /** 실패 시 예외 클래스/메시지 또는 에러 응답 본문 일부 (최대 4000자 절단). */
    @Column(name = "error_detail", columnDefinition = "TEXT")
    val errorDetail: String? = null,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: LocalDateTime,

    @Column(name = "completed_at", nullable = false)
    val completedAt: LocalDateTime
)
