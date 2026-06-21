package com.otoki.powersales.external.common.outboundlog.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

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
@DomainName("외부API호출로그")
@Entity
@Table(name = "external_api_log")
class ExternalApiLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("외부API호출로그ID")
    @Column(name = "external_api_log_id")
    val id: Long = 0,

    /** 호출 대상 외부 시스템 분류 (SAP / SF / NAVER). */
    @FieldName("대상시스템")
    @Column(name = "target_system", nullable = false, length = 20)
    val targetSystem: String,

    /**
     * 호출 종류 식별 key — uri 를 [ExternalApiEndpointKeyResolver] 로 해석한 값.
     *
     * web "외부 API 테스트" 페이지의 탭 key 와 1:1 일치하여, 각 탭이 자기 호출 이력만 정확히 조회한다.
     * resolver 가 분류하지 못한 호출은 null (전체 조회로만 노출).
     */
    @FieldName("엔드포인트키")
    @Column(name = "endpoint_key", length = 50)
    val endpointKey: String? = null,

    @FieldName("HTTP메서드")
    @Column(name = "http_method", nullable = false, length = 10)
    val httpMethod: String,

    /** 호출 URI (query string 포함, 최대 1000자 절단). */
    @FieldName("호출URI")
    @Column(name = "uri", nullable = false, length = 1000)
    val uri: String,

    /** HTTP 응답 상태 코드. 네트워크/연결 실패로 응답이 없으면 null. */
    @FieldName("HTTP상태코드")
    @Column(name = "http_status")
    val httpStatus: Int? = null,

    /** 호출 성공 여부 — 2xx 응답이면 true, 그 외(4xx/5xx/네트워크 예외)면 false. */
    @FieldName("성공여부")
    @Column(name = "success", nullable = false)
    val success: Boolean,

    @FieldName("소요시간(ms)")
    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long,

    /** 실패 시 예외 클래스/메시지 또는 에러 응답 본문 일부 (최대 4000자 절단). */
    @FieldName("오류상세")
    @Column(name = "error_detail", columnDefinition = "TEXT")
    val errorDetail: String? = null,

    /**
     * 요청 본문 — local / dev profile 에서만 적재 (최대 10000자 절단). 그 외 환경/캡처 불가 시 null.
     * 운영(prod)은 PII/용량 보호를 위해 기록하지 않는다.
     */
    @FieldName("요청본문")
    @Column(name = "request_body", columnDefinition = "TEXT")
    val requestBody: String? = null,

    /**
     * 응답 본문 — local / dev profile 에서만 적재 (최대 10000자 절단). 그 외 환경/캡처 불가 시 null.
     * 에러 응답(4xx/5xx)의 본문도 동일하게 기록한다.
     */
    @FieldName("응답본문")
    @Column(name = "response_body", columnDefinition = "TEXT")
    val responseBody: String? = null,

    @FieldName("요청시각")
    @Column(name = "requested_at", nullable = false)
    val requestedAt: LocalDateTime,

    @FieldName("완료시각")
    @Column(name = "completed_at", nullable = false)
    val completedAt: LocalDateTime
)
