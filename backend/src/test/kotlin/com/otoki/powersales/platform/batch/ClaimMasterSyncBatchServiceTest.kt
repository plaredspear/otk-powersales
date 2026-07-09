package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.claim.service.AdminClaimMasterSyncTestService
import com.otoki.powersales.domain.activity.claim.service.AdminLogisticsClaimMasterSyncTestService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ClaimMasterSyncBatchService - 클레임/물류클레임 상태 업데이트 오케스트레이션")
class ClaimMasterSyncBatchServiceTest {

    private val claimSyncService: AdminClaimMasterSyncTestService = mockk()
    private val logisticsSyncService: AdminLogisticsClaimMasterSyncTestService = mockk()

    private fun service(claimEnabled: Boolean = true, logisticsEnabled: Boolean = true) =
        ClaimMasterSyncBatchService(claimSyncService, logisticsSyncService, claimEnabled, logisticsEnabled)

    private fun claimResult(fetched: Int, updated: Int, notFound: Int = 0, skipped: Int = 0) =
        AdminClaimMasterSyncTestService.UpdateResult(fetched, updated, notFound, skipped)

    private fun logisticsResult(fetched: Int, updated: Int, notFound: Int = 0, skipped: Int = 0) =
        AdminLogisticsClaimMasterSyncTestService.UpdateResult(fetched, updated, notFound, skipped)

    @Suppress("UNCHECKED_CAST")
    private fun ScheduledJobRunContext.domain(key: String) =
        pendingMetadata()!![key] as Map<String, Any?>

    @Test
    fun `두 도메인 sync 후 metadata 에 도메인별 집계 기록`() {
        every { claimSyncService.sync(any(), any()) } returns claimResult(fetched = 5, updated = 4, notFound = 1)
        every { logisticsSyncService.sync(any(), any()) } returns logisticsResult(fetched = 3, updated = 3)

        val ctx = ScheduledJobRunContext(ClaimMasterSyncBatch.JOB_NAME)
        service().syncAll(ctx)

        val claim = ctx.domain("claim")
        val logistics = ctx.domain("logistics")
        assertThat(claim["enabled"]).isEqualTo(true)
        assertThat(claim["fetched"]).isEqualTo(5)
        assertThat(claim["updated"]).isEqualTo(4)
        assertThat(claim["notFound"]).isEqualTo(1)
        assertThat(logistics["enabled"]).isEqualTo(true)
        assertThat(logistics["fetched"]).isEqualTo(3)
        assertThat(logistics["updated"]).isEqualTo(3)
    }

    @Test
    fun `꺼진 도메인은 SF 호출 없이 skip 되고 metadata enabled=false`() {
        every { logisticsSyncService.sync(any(), any()) } returns logisticsResult(fetched = 2, updated = 2)

        val ctx = ScheduledJobRunContext(ClaimMasterSyncBatch.JOB_NAME)
        service(claimEnabled = false, logisticsEnabled = true).syncAll(ctx)

        // claim 도메인은 서비스 호출 자체가 없어야 한다.
        verify(exactly = 0) { claimSyncService.sync(any(), any()) }
        verify { logisticsSyncService.sync(any(), any()) }

        val claim = ctx.domain("claim")
        assertThat(claim["enabled"]).isEqualTo(false)
        assertThat(claim["error"]).isEqualTo(false)
        assertThat(ctx.domain("logistics")["enabled"]).isEqualTo(true)
    }

    @Test
    fun `한 도메인 sync 예외가 다른 도메인 처리를 막지 않는다 (도메인 격리 + error 플래그)`() {
        every { claimSyncService.sync(any(), any()) } throws RuntimeException("SF 순단")
        every { logisticsSyncService.sync(any(), any()) } returns logisticsResult(fetched = 1, updated = 1)

        val ctx = ScheduledJobRunContext(ClaimMasterSyncBatch.JOB_NAME)
        service().syncAll(ctx)

        // 물류클레임은 정상 처리됨.
        verify { logisticsSyncService.sync(any(), any()) }
        assertThat(ctx.domain("claim")["error"]).isEqualTo(true)
        assertThat(ctx.domain("logistics")["updated"]).isEqualTo(1)
    }
}
