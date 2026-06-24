package com.otoki.powersales.admin.service

import com.otoki.powersales.domain.activity.promotion.service.PPTMasterBatchService
import com.otoki.powersales.platform.batch.PPTMasterExpireBatch
import com.otoki.powersales.platform.batch.PPTMasterSyncBatch
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PPTMasterManualTriggerService 테스트")
class PPTMasterManualTriggerServiceTest {

    private val pptMasterBatchService = mockk<PPTMasterBatchService>(relaxed = true)
    private val scheduledJobRunner = mockk<ScheduledJobRunner>()
    private lateinit var service: PPTMasterManualTriggerService

    @BeforeEach
    fun setUp() {
        service = PPTMasterManualTriggerService(pptMasterBatchService, scheduledJobRunner)

        // ScheduledJobRunner.run 은 jobName 으로 ctx 를 만들어 본문 람다를 그대로 실행한다고 가정한다
        // (이력 기록 부수효과는 Runner 자체 테스트에서 검증하므로 여기선 람다 실행만 흉내낸다).
        every { scheduledJobRunner.run<Unit>(any(), any()) } answers {
            val jobName = firstArg<String>()
            val block = secondArg<(ScheduledJobRunContext) -> Unit>()
            block(ScheduledJobRunContext(jobName))
        }
    }

    @Test
    @DisplayName("expire jobName 은 expireMasters 를 triggeredBy=MANUAL 로 호출한다")
    fun trigger_expire_callsExpireMastersWithManual() {
        val response = service.trigger(PPTMasterExpireBatch.JOB_NAME)

        verify(exactly = 1) {
            pptMasterBatchService.expireMasters(any(), PPTMasterManualTriggerService.TRIGGERED_BY_MANUAL)
        }
        verify(exactly = 0) { pptMasterBatchService.syncValidMasters(any(), any()) }
        assertThat(response.jobName).isEqualTo(PPTMasterExpireBatch.JOB_NAME)
        assertThat(response.status).isEqualTo("SUCCESS")
    }

    @Test
    @DisplayName("sync jobName 은 syncValidMasters 를 triggeredBy=MANUAL 로 호출한다")
    fun trigger_sync_callsSyncValidMastersWithManual() {
        val response = service.trigger(PPTMasterSyncBatch.JOB_NAME)

        verify(exactly = 1) {
            pptMasterBatchService.syncValidMasters(any(), PPTMasterManualTriggerService.TRIGGERED_BY_MANUAL)
        }
        verify(exactly = 0) { pptMasterBatchService.expireMasters(any(), any()) }
        assertThat(response.jobName).isEqualTo(PPTMasterSyncBatch.JOB_NAME)
        assertThat(response.status).isEqualTo("SUCCESS")
    }

    @Test
    @DisplayName("지원하지 않는 jobName 은 IllegalArgumentException 을 던지고 배치를 실행하지 않는다")
    fun trigger_unsupported_throws() {
        assertThatThrownBy { service.trigger("sap-outbox-worker") }
            .isInstanceOf(IllegalArgumentException::class.java)

        verify(exactly = 0) { scheduledJobRunner.run<Unit>(any(), any()) }
        verify(exactly = 0) { pptMasterBatchService.expireMasters(any(), any()) }
        verify(exactly = 0) { pptMasterBatchService.syncValidMasters(any(), any()) }
    }

    @Test
    @DisplayName("supports 는 두 PPT jobName 만 true 를 반환한다")
    fun supports_onlyPptJobs() {
        assertThat(service.supports(PPTMasterExpireBatch.JOB_NAME)).isTrue()
        assertThat(service.supports(PPTMasterSyncBatch.JOB_NAME)).isTrue()
        assertThat(service.supports("sap-outbox-worker")).isFalse()
    }
}
