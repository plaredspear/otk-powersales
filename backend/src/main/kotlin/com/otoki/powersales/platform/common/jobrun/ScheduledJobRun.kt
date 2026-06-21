package com.otoki.powersales.platform.common.jobrun

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * 시각 필드는 UTC wall clock 으로 저장된다 (전사 컨벤션 — 스펙 #564).
 */
@DomainName("스케줄잡실행이력")
@Entity
@Table(name = "scheduled_job_run")
class ScheduledJobRun(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "job_name", nullable = false, length = 64)
    val jobName: String,

    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime,

    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    @Column(name = "status", nullable = false, length = 16)
    var status: String = STATUS_RUNNING,

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    var errorMessage: String? = null,

    // Hibernate JSON 매핑: PostgreSQL 에서는 jsonb, H2 에서는 JSON 으로 생성된다.
    // String 필드에 JSON 직렬화된 값을 그대로 보관한다 (Runner 가 ObjectMapper 로 직렬화).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    var metadata: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = startedAt,
) {
    companion object {
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILURE = "FAILURE"
        const val ERROR_MESSAGE_MAX_LENGTH = 4000
    }
}
