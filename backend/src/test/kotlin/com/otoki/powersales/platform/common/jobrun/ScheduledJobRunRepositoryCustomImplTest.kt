package com.otoki.powersales.platform.common.jobrun

import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRun
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("ScheduledJobRunRepositoryCustomImpl 테스트")
class ScheduledJobRunRepositoryCustomImplTest {

    @Autowired private lateinit var repository: ScheduledJobRunRepository
    @Autowired private lateinit var em: TestEntityManager

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        em.clear()
    }

    private fun persist(
        jobName: String,
        status: String,
        startedAt: LocalDateTime,
        endedAt: LocalDateTime? = null,
        errorMessage: String? = null,
    ): ScheduledJobRun {
        val row = ScheduledJobRun(
            jobName = jobName,
            startedAt = startedAt,
            endedAt = endedAt,
            status = status,
            errorMessage = errorMessage,
            createdAt = startedAt,
        )
        em.persistAndFlush(row)
        return row
    }

    @Test
    @DisplayName("search - jobName + status + 기간 필터 조합 시 결과가 좁혀지고 startedAt DESC 정렬")
    fun search_filterCombination() {
        val baseTime = LocalDateTime.of(2026, 5, 18, 0, 0, 0)
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(1, ChronoUnit.HOURS))
        persist("jobA", ScheduledJobRun.STATUS_FAILURE, baseTime.plus(2, ChronoUnit.HOURS))
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(3, ChronoUnit.HOURS))
        persist("jobB", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(4, ChronoUnit.HOURS))

        val page = repository.search(
            jobName = "jobA",
            status = ScheduledJobRun.STATUS_SUCCESS,
            from = baseTime,
            to = baseTime.plus(5, ChronoUnit.HOURS),
            pageable = PageRequest.of(0, 10),
        )

        assertThat(page.totalElements).isEqualTo(2L)
        assertThat(page.content).hasSize(2)
        assertThat(page.content[0].startedAt).isEqualTo(baseTime.plus(3, ChronoUnit.HOURS))
        assertThat(page.content[1].startedAt).isEqualTo(baseTime.plus(1, ChronoUnit.HOURS))
    }

    @Test
    @DisplayName("search - 페이지네이션 동작 (size=2 인 경우 첫 페이지 2건 / totalCount 정확)")
    fun search_pagination() {
        val baseTime = LocalDateTime.of(2026, 5, 18, 0, 0, 0)
        repeat(5) { i ->
            persist("jobC", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(i.toLong(), ChronoUnit.MINUTES))
        }

        val page = repository.search(
            jobName = null,
            status = null,
            from = null,
            to = null,
            pageable = PageRequest.of(0, 2),
        )

        assertThat(page.totalElements).isEqualTo(5L)
        assertThat(page.content).hasSize(2)
        assertThat(page.content[0].startedAt).isEqualTo(baseTime.plus(4, ChronoUnit.MINUTES))
    }

    @Test
    @DisplayName("countByStatusWithin - status 별 group-by 카운트가 정확하다")
    fun countByStatusWithin_groupBy() {
        val baseTime = LocalDateTime.of(2026, 5, 18, 0, 0, 0)
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(1, ChronoUnit.HOURS))
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(2, ChronoUnit.HOURS))
        persist("jobB", ScheduledJobRun.STATUS_FAILURE, baseTime.plus(3, ChronoUnit.HOURS))
        persist("jobB", ScheduledJobRun.STATUS_RUNNING, baseTime.plus(4, ChronoUnit.HOURS))
        // 윈도우 밖
        persist("jobC", ScheduledJobRun.STATUS_SUCCESS, baseTime.minus(1, ChronoUnit.HOURS))

        val counts = repository.countByStatusWithin(baseTime, baseTime.plus(24, ChronoUnit.HOURS))

        assertThat(counts[ScheduledJobRun.STATUS_SUCCESS]).isEqualTo(2L)
        assertThat(counts[ScheduledJobRun.STATUS_FAILURE]).isEqualTo(1L)
        assertThat(counts[ScheduledJobRun.STATUS_RUNNING]).isEqualTo(1L)
    }

    @Test
    @DisplayName("aggregateByJobNameWithin - 잡별 status 카운트 + 마지막 실행이 정확하고 윈도우 밖/미대상 잡은 제외")
    fun aggregateByJobNameWithin_perJobCounts() {
        val baseTime = LocalDateTime.of(2026, 7, 14, 22, 0, 0)
        val to = LocalDateTime.of(2026, 7, 15, 22, 0, 0)

        // jobA: 성공 2 + 실패 1 + 스킵 1. 마지막 실행은 가장 늦은 시각(성공).
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(1, ChronoUnit.HOURS))
        persist("jobA", ScheduledJobRun.STATUS_FAILURE, baseTime.plus(2, ChronoUnit.HOURS))
        persist("jobA", ScheduledJobRun.STATUS_SKIPPED, baseTime.plus(3, ChronoUnit.HOURS))
        val jobALast = persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(4, ChronoUnit.HOURS))
        // jobB: 실행중 1.
        persist("jobB", ScheduledJobRun.STATUS_RUNNING, baseTime.plus(5, ChronoUnit.HOURS))
        // 윈도우 경계 밖 (to 이후) — 제외돼야 함.
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, to.plus(1, ChronoUnit.MINUTES))
        // 대상 목록에 없는 잡 — 제외돼야 함.
        persist("jobZ", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(1, ChronoUnit.HOURS))

        val result = repository.aggregateByJobNameWithin(listOf("jobA", "jobB"), baseTime, to)
            .associateBy { it.jobName }

        assertThat(result).containsOnlyKeys("jobA", "jobB")

        val jobA = result.getValue("jobA")
        assertThat(jobA.totalCount).isEqualTo(4L)
        assertThat(jobA.successCount).isEqualTo(2L)
        assertThat(jobA.failureCount).isEqualTo(1L)
        assertThat(jobA.skippedCount).isEqualTo(1L)
        assertThat(jobA.runningCount).isEqualTo(0L)
        assertThat(jobA.lastStartedAt).isEqualTo(jobALast.startedAt)
        assertThat(jobA.lastStatus).isEqualTo(ScheduledJobRun.STATUS_SUCCESS)

        val jobB = result.getValue("jobB")
        assertThat(jobB.totalCount).isEqualTo(1L)
        assertThat(jobB.runningCount).isEqualTo(1L)
        assertThat(jobB.lastStatus).isEqualTo(ScheduledJobRun.STATUS_RUNNING)
    }

    @Test
    @DisplayName("aggregateByJobNameWithin - 이력이 0건인 잡은 결과에 포함되지 않는다")
    fun aggregateByJobNameWithin_noRowsExcluded() {
        val baseTime = LocalDateTime.of(2026, 7, 14, 22, 0, 0)
        val to = LocalDateTime.of(2026, 7, 15, 22, 0, 0)
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(1, ChronoUnit.HOURS))

        val result = repository.aggregateByJobNameWithin(listOf("jobA", "jobNoHistory"), baseTime, to)

        assertThat(result).hasSize(1)
        assertThat(result[0].jobName).isEqualTo("jobA")
    }

    @Test
    @DisplayName("findDistinctJobNames - 중복 제거 + 가나다순 정렬")
    fun findDistinctJobNames_distinctSorted() {
        val baseTime = LocalDateTime.of(2026, 5, 18, 0, 0, 0)
        persist("zeta-job", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(1, ChronoUnit.MINUTES))
        persist("alpha-job", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(2, ChronoUnit.MINUTES))
        persist("alpha-job", ScheduledJobRun.STATUS_FAILURE, baseTime.plus(3, ChronoUnit.MINUTES))
        persist("beta-job", ScheduledJobRun.STATUS_SUCCESS, baseTime.plus(4, ChronoUnit.MINUTES))

        val names = repository.findDistinctJobNames()

        assertThat(names).containsExactly("alpha-job", "beta-job", "zeta-job")
    }
}
