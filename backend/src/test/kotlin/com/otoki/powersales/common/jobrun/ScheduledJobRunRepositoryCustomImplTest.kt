package com.otoki.powersales.common.jobrun

import com.otoki.powersales.common.config.QueryDslConfig
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
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusHours(1))
        persist("jobA", ScheduledJobRun.STATUS_FAILURE, baseTime.plusHours(2))
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusHours(3))
        persist("jobB", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusHours(4))

        val page = repository.search(
            jobName = "jobA",
            status = ScheduledJobRun.STATUS_SUCCESS,
            from = baseTime,
            to = baseTime.plusHours(5),
            pageable = PageRequest.of(0, 10),
        )

        assertThat(page.totalElements).isEqualTo(2L)
        assertThat(page.content).hasSize(2)
        assertThat(page.content[0].startedAt).isEqualTo(baseTime.plusHours(3))
        assertThat(page.content[1].startedAt).isEqualTo(baseTime.plusHours(1))
    }

    @Test
    @DisplayName("search - 페이지네이션 동작 (size=2 인 경우 첫 페이지 2건 / totalCount 정확)")
    fun search_pagination() {
        val baseTime = LocalDateTime.of(2026, 5, 18, 0, 0, 0)
        repeat(5) { i ->
            persist("jobC", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusMinutes(i.toLong()))
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
        assertThat(page.content[0].startedAt).isEqualTo(baseTime.plusMinutes(4))
    }

    @Test
    @DisplayName("countByStatusWithin - status 별 group-by 카운트가 정확하다")
    fun countByStatusWithin_groupBy() {
        val baseTime = LocalDateTime.of(2026, 5, 18, 0, 0, 0)
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusHours(1))
        persist("jobA", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusHours(2))
        persist("jobB", ScheduledJobRun.STATUS_FAILURE, baseTime.plusHours(3))
        persist("jobB", ScheduledJobRun.STATUS_RUNNING, baseTime.plusHours(4))
        // 윈도우 밖
        persist("jobC", ScheduledJobRun.STATUS_SUCCESS, baseTime.minusHours(1))

        val counts = repository.countByStatusWithin(baseTime, baseTime.plusHours(24))

        assertThat(counts[ScheduledJobRun.STATUS_SUCCESS]).isEqualTo(2L)
        assertThat(counts[ScheduledJobRun.STATUS_FAILURE]).isEqualTo(1L)
        assertThat(counts[ScheduledJobRun.STATUS_RUNNING]).isEqualTo(1L)
    }

    @Test
    @DisplayName("findDistinctJobNames - 중복 제거 + 가나다순 정렬")
    fun findDistinctJobNames_distinctSorted() {
        val baseTime = LocalDateTime.of(2026, 5, 18, 0, 0, 0)
        persist("zeta-job", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusMinutes(1))
        persist("alpha-job", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusMinutes(2))
        persist("alpha-job", ScheduledJobRun.STATUS_FAILURE, baseTime.plusMinutes(3))
        persist("beta-job", ScheduledJobRun.STATUS_SUCCESS, baseTime.plusMinutes(4))

        val names = repository.findDistinctJobNames()

        assertThat(names).containsExactly("alpha-job", "beta-job", "zeta-job")
    }
}
