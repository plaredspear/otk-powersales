package com.otoki.powersales.platform.common.jobrun

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.platform.batch.toggle.ScheduledJobToggleStore
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRun
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunCleanupService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunRepository
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.get

/**
 * `ScheduledJobRunner` 통합 테스트 (스펙 #548 §9, 시각 타입 정렬 #564).
 *
 * - `@DataJpaTest` + H2 (PostgreSQL 호환 모드 미사용) 위에서 실제 INSERT/UPDATE 트랜잭션 검증.
 * - `@Import` 로 Runner / CleanupService / QueryDslConfig 를 한정적으로 가져온다.
 * - ObjectMapper 는 Spring Boot 가 자동 구성하지 않는 슬라이스라 명시 빈으로 등록.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(
    ScheduledJobRunner::class,
    ScheduledJobRunCleanupService::class,
    QueryDslConfig::class,
    ScheduledJobRunnerIntegrationTest.TestObjectMapperConfig::class,
)
@DisplayName("ScheduledJobRunner 통합 테스트")
class ScheduledJobRunnerIntegrationTest {

    @Autowired
    private lateinit var runner: ScheduledJobRunner

    @Autowired
    private lateinit var cleanupService: ScheduledJobRunCleanupService

    @Autowired
    private lateinit var repository: ScheduledJobRunRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var toggleStore: ScheduledJobToggleStore

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        // 기본: 모든 잡 런타임 활성 (개별 테스트에서 override).
        every { toggleStore.isEnabled(any()) } returns true
    }

    @Nested
    @DisplayName("Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("정상 종료 - row 1건이 SUCCESS 로 기록")
        fun successRecorded() {
            val before = LocalDateTime.now()
            val returnValue = runner.run("test.success") { 42 }

            assertThat(returnValue).isEqualTo(42)
            val rows = repository.findAll()
            assertThat(rows).hasSize(1)
            val row = rows.single()
            assertThat(row.jobName).isEqualTo("test.success")
            assertThat(row.status).isEqualTo(ScheduledJobRun.STATUS_SUCCESS)
            assertThat(row.endedAt).isNotNull
            assertThat(row.endedAt).isAfterOrEqualTo(row.startedAt)
            assertThat(row.startedAt).isAfterOrEqualTo(before.minusSeconds(1))
            assertThat(row.errorMessage).isNull()
            assertThat(row.metadata).isNull()
        }

        @Test
        @DisplayName("metadata 등록 - context.metadata() 호출 -> JSON 컬럼에 직렬화 저장")
        fun metadataRegistered() {
            runner.run("test.metadata") { context ->
                context.metadata(mapOf("processed" to 5, "skipped" to 1))
            }

            val row = repository.findAll().single()
            assertThat(row.status).isEqualTo(ScheduledJobRun.STATUS_SUCCESS)
            assertThat(row.metadata).isNotNull
            val parsed = objectMapper.readValue(row.metadata, Map::class.java)
            assertThat(parsed["processed"]).isEqualTo(5)
            assertThat(parsed["skipped"]).isEqualTo(1)
        }

        @Test
        @DisplayName("두 번 연속 호출 - 동일 job_name 으로 row 2건 별도 기록")
        fun twoSequentialRunsCreateTwoRows() {
            runner.run("test.repeat") { /* no-op */ }
            runner.run("test.repeat") { /* no-op */ }

            val rows = repository.findAll()
            assertThat(rows).hasSize(2)
            assertThat(rows.map { it.status }).containsOnly(ScheduledJobRun.STATUS_SUCCESS)
            // started_at 으로 식별 가능해야 함 (서로 다른 시각)
            val startedAts = rows.map { it.startedAt }.toSet()
            assertThat(startedAts).hasSize(2)
        }
    }

    @Nested
    @DisplayName("Error Path")
    inner class ErrorPath {

        @Test
        @DisplayName("본문 RuntimeException - row 1건 FAILURE 기록 + 예외 전파")
        fun runtimeExceptionPropagatesAndRecordsFailure() {
            assertThatThrownBy {
                runner.run<Unit>("test.boom") {
                    throw IllegalStateException("boom")
                }
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("boom")

            val row = repository.findAll().single()
            assertThat(row.jobName).isEqualTo("test.boom")
            assertThat(row.status).isEqualTo(ScheduledJobRun.STATUS_FAILURE)
            assertThat(row.endedAt).isNotNull
            assertThat(row.errorMessage).isEqualTo("IllegalStateException: boom")
        }

        @Test
        @DisplayName("긴 메시지 트렁케이트 - 5000자 메시지가 정확히 4000자로 잘림")
        fun longErrorMessageIsTruncated() {
            val longMessage = "x".repeat(5000)

            assertThatThrownBy {
                runner.run<Unit>("test.longMessage") {
                    throw RuntimeException(longMessage)
                }
            }.isInstanceOf(RuntimeException::class.java)

            val row = repository.findAll().single()
            assertThat(row.status).isEqualTo(ScheduledJobRun.STATUS_FAILURE)
            assertThat(row.errorMessage).hasSize(ScheduledJobRun.ERROR_MESSAGE_MAX_LENGTH)
            // 트렁케이트 결과는 prefix("RuntimeException: ") + 'x' 로 채워진다.
            assertThat(row.errorMessage).startsWith("RuntimeException: x")
        }
    }

    @Nested
    @DisplayName("ScheduledJobRunCleanup - 보존 정책")
    inner class CleanupTests {

        @Test
        @DisplayName("90일 초과 row 삭제 - cleanup 자기 row 1건만 SUCCESS 로 남고 metadata.deleted 기록")
        fun cleanupRemovesExpiredRows() {
            // 90일을 초과한 오래된 row 5건을 직접 INSERT (Runner 우회)
            val oldThreshold = LocalDateTime.now().minus(ScheduledJobRunner.RETENTION_DAYS + 1, ChronoUnit.DAYS)
            repeat(5) {
                repository.save(
                    ScheduledJobRun(
                        jobName = "old.job",
                        startedAt = oldThreshold,
                        status = ScheduledJobRun.STATUS_SUCCESS,
                        endedAt = oldThreshold,
                        createdAt = oldThreshold,
                    )
                )
            }
            // 보존 기간 이내(최근) row 1건도 추가 — 삭제 대상이 아님을 검증
            val recent = LocalDateTime.now().minus(1, ChronoUnit.DAYS)
            repository.save(
                ScheduledJobRun(
                    jobName = "recent.job",
                    startedAt = recent,
                    status = ScheduledJobRun.STATUS_SUCCESS,
                    endedAt = recent,
                    createdAt = recent,
                )
            )
            assertThat(repository.count()).isEqualTo(6)

            runner.run("scheduledJobRun.cleanup") { ctx ->
                cleanupService.cleanup(ctx)
            }

            // 결과: 삭제 5건 + cleanup 자기 이력 1건 + 최근 row 1건 = 2건 남음
            val remaining = repository.findAll()
            assertThat(remaining).hasSize(2)
            val cleanupRow = remaining.first { it.jobName == "scheduledJobRun.cleanup" }
            assertThat(cleanupRow.status).isEqualTo(ScheduledJobRun.STATUS_SUCCESS)
            assertThat(cleanupRow.metadata).isNotNull
            val parsed = objectMapper.readValue(cleanupRow.metadata, Map::class.java)
            assertThat((parsed["deleted"] as Number).toInt()).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("runScheduled - 런타임 토글")
    inner class RunScheduledToggle {

        @Test
        @DisplayName("활성 - 본문 실행 + SUCCESS 기록")
        fun enabledRunsBody() {
            every { toggleStore.isEnabled("test.toggle") } returns true
            var executed = false

            runner.runScheduled("test.toggle") { executed = true }

            assertThat(executed).isTrue()
            val row = repository.findAll().single()
            assertThat(row.status).isEqualTo(ScheduledJobRun.STATUS_SUCCESS)
        }

        @Test
        @DisplayName("비활성 - 본문 미실행 + SKIPPED row 1건 기록")
        fun disabledSkipsBody() {
            every { toggleStore.isEnabled("test.toggle") } returns false
            var executed = false

            runner.runScheduled("test.toggle") { executed = true }

            assertThat(executed).isFalse()
            val row = repository.findAll().single()
            assertThat(row.jobName).isEqualTo("test.toggle")
            assertThat(row.status).isEqualTo(ScheduledJobRun.STATUS_SKIPPED)
            assertThat(row.endedAt).isNotNull
            assertThat(row.errorMessage).isNull()
        }
    }

    /**
     * `@DataJpaTest` 슬라이스 컨텍스트는 Jackson 을 자동 구성하지 않으므로
     * Runner 가 의존하는 ObjectMapper 를 명시 빈으로 등록한다.
     */
    @Configuration
    class TestObjectMapperConfig {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()
    }
}
