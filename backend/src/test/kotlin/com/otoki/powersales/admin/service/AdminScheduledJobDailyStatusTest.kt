package com.otoki.powersales.admin.service

import com.otoki.powersales.domain.sales.materialize.OroraSalesMaterializeFacade
import com.otoki.powersales.platform.batch.ClaimMasterSyncBatch
import com.otoki.powersales.platform.batch.OroraMonthlySalesMaterializeBatch
import com.otoki.powersales.platform.batch.ScheduledJobCatalog
import com.otoki.powersales.platform.batch.TeamMemberScheduleSapOutboundBatch
import com.otoki.powersales.platform.batch.toggle.ScheduledJobToggleStore
import com.otoki.powersales.platform.common.jobrun.JobRunAggregate
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRun
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ListableBeanFactory
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AdminScheduledJobService.dailyStatus 테스트")
class AdminScheduledJobDailyStatusTest {

    private val repository = mockk<ScheduledJobRunRepository>()
    private val ororaFacade = mockk<OroraSalesMaterializeFacade>()
    private val asyncRunner = mockk<OroraMaterializeAsyncRunner>()
    private val beanFactory = mockk<ListableBeanFactory>()
    private val toggleStore = mockk<ScheduledJobToggleStore>()
    private val cronResolver = mockk<ScheduledJobCronResolver>()

    private val service = AdminScheduledJobService(
        scheduledJobRunRepository = repository,
        ororaSalesMaterializeFacade = ororaFacade,
        ororaMaterializeAsyncRunner = asyncRunner,
        beanFactory = beanFactory,
        scheduledJobToggleStore = toggleStore,
        scheduledJobCronResolver = cronResolver,
    )

    private val date = LocalDate.of(2026, 7, 15)
    private val expectedFrom = LocalDateTime.of(2026, 7, 14, 22, 0)
    private val expectedTo = LocalDateTime.of(2026, 7, 15, 22, 0)

    /** 모든 잡 빈이 등록(활성) 되어 있다고 가정하는 기본 stub. */
    private fun allBeansRegistered() {
        every { beanFactory.getBeanNamesForType(any<Class<*>>()) } returns arrayOf("bean")
    }

    @Test
    @DisplayName("윈도우는 전일 22:00 ~ 당일 22:00 으로 산출되고 대상 잡 순서가 카탈로그 정의와 일치")
    fun windowBoundsAndOrder() {
        allBeansRegistered()
        every { repository.aggregateByJobNameWithin(any(), any(), any()) } returns emptyList()
        every { cronResolver.resolvedCronByJobName() } returns emptyMap()

        val response = service.dailyStatus(date)

        assertThat(response.date).isEqualTo("2026-07-15")
        assertThat(response.windowFrom).isEqualTo(expectedFrom)
        assertThat(response.windowTo).isEqualTo(expectedTo)
        assertThat(response.items.map { it.jobName })
            .containsExactlyElementsOf(DashboardScheduledJobTargets.JOB_NAMES)
    }

    @Test
    @DisplayName("actualCount 는 SKIPPED 를 제외하며 executed 는 실제 실행(SKIPPED 제외) 기준")
    fun actualCountExcludesSkipped() {
        allBeansRegistered()
        val teamJob = TeamMemberScheduleSapOutboundBatch.JOB_NAME
        val claimJob = ClaimMasterSyncBatch.JOB_NAME
        every { repository.aggregateByJobNameWithin(any(), any(), any()) } returns listOf(
            // 여사원일정: 성공 1 + 스킵 2 → actual=1, executed=true
            JobRunAggregate(
                jobName = teamJob,
                totalCount = 3, successCount = 1, failureCount = 0, skippedCount = 2, runningCount = 0,
                lastStartedAt = expectedFrom.plusHours(3), lastStatus = ScheduledJobRun.STATUS_SKIPPED,
            ),
            // 클레임: 스킵만 2 → actual=0, executed=false
            JobRunAggregate(
                jobName = claimJob,
                totalCount = 2, successCount = 0, failureCount = 0, skippedCount = 2, runningCount = 0,
                lastStartedAt = expectedFrom.plusHours(1), lastStatus = ScheduledJobRun.STATUS_SKIPPED,
            ),
        )
        every { cronResolver.resolvedCronByJobName() } returns emptyMap()

        val response = service.dailyStatus(date)
        val team = response.items.first { it.jobName == teamJob }
        val claim = response.items.first { it.jobName == claimJob }

        assertThat(team.actualCount).isEqualTo(1L)
        assertThat(team.totalCount).isEqualTo(3L)
        assertThat(team.executed).isTrue()

        assertThat(claim.actualCount).isEqualTo(0L)
        assertThat(claim.totalCount).isEqualTo(2L)
        assertThat(claim.executed).isFalse()
    }

    @Test
    @DisplayName("expectedCount 는 해석된 cron 으로 계산되고, cron 미등록 잡은 null")
    fun expectedCountFromResolvedCron() {
        allBeansRegistered()
        val claimJob = ClaimMasterSyncBatch.JOB_NAME
        every { repository.aggregateByJobNameWithin(any(), any(), any()) } returns emptyList()
        // 클레임만 cron 등록 (매시간). 나머지는 미등록 → expectedCount=null.
        every { cronResolver.resolvedCronByJobName() } returns mapOf(claimJob to "0 0 * * * *")
        // 매시간 정각 = 24시간 윈도우에서 24회 (실제 계산 로직은 ScheduledJobCronResolverTest 에서 검증).
        every { cronResolver.expectedFireCount("0 0 * * * *", expectedFrom, expectedTo) } returns 24

        val response = service.dailyStatus(date)
        val claim = response.items.first { it.jobName == claimJob }
        val other = response.items.first { it.jobName == TeamMemberScheduleSapOutboundBatch.JOB_NAME }

        // 매시간 정각 = 24시간 윈도우에서 24회.
        assertThat(claim.expectedCount).isEqualTo(24)
        assertThat(other.expectedCount).isNull()
    }

    @Test
    @DisplayName("빈 미등록 잡은 enabled=false")
    fun disabledWhenBeanMissing() {
        // 여사원일정 배치 빈만 없음, 나머지는 있음.
        val teamBeanType = ScheduledJobCatalog.ENTRIES
            .first { it.jobName == TeamMemberScheduleSapOutboundBatch.JOB_NAME }.beanType
        every { beanFactory.getBeanNamesForType(teamBeanType) } returns emptyArray()
        every { beanFactory.getBeanNamesForType(not(teamBeanType)) } returns arrayOf("bean")
        every { repository.aggregateByJobNameWithin(any(), any(), any()) } returns emptyList()
        every { cronResolver.resolvedCronByJobName() } returns emptyMap()

        val response = service.dailyStatus(date)
        val team = response.items.first { it.jobName == TeamMemberScheduleSapOutboundBatch.JOB_NAME }
        val claim = response.items.first { it.jobName == ClaimMasterSyncBatch.JOB_NAME }

        assertThat(team.enabled).isFalse()
        assertThat(claim.enabled).isTrue()
    }

    @Test
    @DisplayName("ORORA 월매출은 note(매월 3일 안내)를 포함한다")
    fun ororaMonthlyHasNote() {
        allBeansRegistered()
        every { repository.aggregateByJobNameWithin(any(), any(), any()) } returns emptyList()
        every { cronResolver.resolvedCronByJobName() } returns emptyMap()

        val response = service.dailyStatus(date)
        val monthly = response.items.first { it.jobName == OroraMonthlySalesMaterializeBatch.JOB_NAME }

        assertThat(monthly.note).isNotNull()
        assertThat(monthly.note).contains("3일")
    }
}
