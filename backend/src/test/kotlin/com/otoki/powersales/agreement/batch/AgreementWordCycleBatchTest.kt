package com.otoki.powersales.agreement.batch

import com.otoki.powersales.agreement.service.AgreementWordCycleService
import com.otoki.powersales.common.jobrun.ScheduledJobRunContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("AgreementWordCycleBatch — GPS 재동의 cycle daily 배치 (#654)")
class AgreementWordCycleBatchTest {

    private val service: AgreementWordCycleService = mock()
    private val runner = mock<com.otoki.powersales.common.jobrun.ScheduledJobRunner>()
    private val batch = AgreementWordCycleBatch(service = service, scheduledJobRunner = runner)

    @Test
    @DisplayName("execute — service 호출 + context metadata (branch / resetCount) 적재")
    fun execute_invokesServiceAndRecordsMetadata() {
        whenever(service.runCycle()).thenReturn(
            AgreementWordCycleService.Result(
                branch = AgreementWordCycleService.Branch.ROTATION,
                resetCount = 42L
            )
        )
        val context = mock<ScheduledJobRunContext>()

        batch.execute(context)

        val captor = argumentCaptor<Map<String, Any?>>()
        verify(context).metadata(captor.capture())
        val metadata = captor.firstValue
        assertThat(metadata["branch"]).isEqualTo("ROTATION")
        assertThat(metadata["resetCount"]).isEqualTo(42L)
    }

    @Test
    @DisplayName("execute — NO_OP 분기에도 service 1회 호출 + metadata 기록")
    fun execute_noOpStillRecordsMetadata() {
        whenever(service.runCycle()).thenReturn(
            AgreementWordCycleService.Result(
                branch = AgreementWordCycleService.Branch.NO_OP,
                resetCount = 0L
            )
        )
        val context = mock<ScheduledJobRunContext>()

        batch.execute(context)

        verify(service).runCycle()
        val captor = argumentCaptor<Map<String, Any?>>()
        verify(context).metadata(captor.capture())
        assertThat(captor.firstValue["branch"]).isEqualTo("NO_OP")
        assertThat(captor.firstValue["resetCount"]).isEqualTo(0L)
    }

    @Test
    @DisplayName("execute — context null 인 경우 metadata 호출 없이 정상 종료")
    fun execute_nullContextDoesNotThrow() {
        whenever(service.runCycle()).thenReturn(
            AgreementWordCycleService.Result(
                branch = AgreementWordCycleService.Branch.NEW_ONLY,
                resetCount = 5L
            )
        )

        batch.execute(context = null)

        verify(service).runCycle()
    }
}
