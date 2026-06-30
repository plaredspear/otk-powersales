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
import com.otoki.powersales.platform.common.entity.FieldName

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

    @FieldName("직무명")
    @Column(name = "job_name", nullable = false, length = 64)
    val jobName: String,

    @FieldName("시작시각")
    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime,

    @FieldName("종료시각")
    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    @FieldName("상태")
    @Column(name = "status", nullable = false, length = 16)
    var status: String = STATUS_RUNNING,

    @FieldName("오류메시지")
    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    var errorMessage: String? = null,

    // Hibernate JSON 매핑: PostgreSQL 에서는 jsonb, H2 에서는 JSON 으로 생성된다.
    // String 필드에 JSON 직렬화된 값을 그대로 보관한다 (Runner 가 ObjectMapper 로 직렬화).
    @JdbcTypeCode(SqlTypes.JSON)
    @FieldName("메타데이터")
    @Column(name = "metadata")
    var metadata: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = startedAt,
) {
    companion object {
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILURE = "FAILURE"

        /** 런타임 토글 비활성 상태라 본문을 실행하지 않고 건너뛴 경우 (스케줄 잡 토글). */
        const val STATUS_SKIPPED = "SKIPPED"

        const val ERROR_MESSAGE_MAX_LENGTH = 4000
    }
}
