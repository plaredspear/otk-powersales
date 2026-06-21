package com.otoki.powersales.external.sap.outbox

import com.otoki.powersales.platform.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 범용 SAP outbound 송신 큐 (Spec #592).
 *
 * 도메인 무관 — `domain_type` + `aggregate_id` + `interface_id` + `payload` poly 구조.
 * 향후 SAP outbound 신규 도메인이 동일 테이블 + 동일 워커([SapOutboxBatchService])를 재사용한다.
 *
 * **상태 전이** (Outbox 자체 status — 도메인 status 와 별도 차원):
 *  - `PENDING` (적재) → 워커 dispatch → `SENT` (송신 성공)
 *  - `PENDING` → 워커 dispatch → `FAILED` (송신 실패, 재시도 한도 초과) / `RETRY` (재시도 대기)
 *  - `RETRY` → 다음 폴링 사이클에서 재시도
 *
 * `payload` 는 SAP 송신 페이로드를 JSON 문자열로 직렬화해 저장한다 (PostgreSQL `jsonb` / H2 `JSON`).
 * 도메인 sender 가 `ObjectMapper` 로 직렬화한 String 을 적재.
 */
@DomainName("SAP송신큐")
@Entity
@Table(
    name = "sap_outbox",
    indexes = [
        Index(name = "idx_sap_outbox_status_created_at", columnList = "status, created_at"),
        Index(name = "idx_sap_outbox_domain_aggregate", columnList = "domain_type, aggregate_id"),
    ],
)
class SapOutbox(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("SAP송신큐ID")
    @Column(name = "sap_outbox_id")
    val id: Long = 0,

    @FieldName("도메인유형")
    @Column(name = "domain_type", nullable = false, length = 50)
    val domainType: String,

    @FieldName("집계대상ID")
    @Column(name = "aggregate_id", nullable = false)
    val aggregateId: Long,

    @FieldName("인터페이스ID")
    @Column(name = "interface_id", nullable = false, length = 80)
    val interfaceId: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @FieldName("페이로드")
    @Column(name = "payload", nullable = false)
    val payload: String,

    @FieldName("상태")
    @Column(name = "status", nullable = false, length = 20)
    var status: String = STATUS_PENDING,

    @FieldName("재시도횟수")
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @FieldName("마지막오류")
    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,

    @FieldName("송신일시")
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,
) : BaseEntity() {

    fun markSent() {
        this.status = STATUS_SENT
        this.sentAt = LocalDateTime.now()
        this.lastError = null
    }

    fun markRetry(error: String?) {
        this.status = STATUS_RETRY
        this.retryCount += 1
        this.lastError = error?.take(MAX_ERROR_LENGTH)
    }

    fun markFailed(error: String?) {
        this.status = STATUS_FAILED
        this.retryCount += 1
        this.lastError = error?.take(MAX_ERROR_LENGTH)
    }

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_RETRY = "RETRY"
        const val STATUS_FAILED = "FAILED"
        const val MAX_ERROR_LENGTH = 4000
        const val MAX_RETRY_COUNT = 5
    }
}
