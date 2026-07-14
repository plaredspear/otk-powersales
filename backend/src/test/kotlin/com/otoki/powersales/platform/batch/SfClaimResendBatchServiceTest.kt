package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.claim.enums.ClaimSfSendStatus
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.domain.activity.claim.service.ClaimSfDispatchService
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus
import com.otoki.powersales.domain.activity.suggestion.repository.SuggestionRepository
import com.otoki.powersales.domain.activity.suggestion.service.SuggestionSfResendService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SfClaimResendBatchService - 제품/물류 클레임 SF 재전송 오케스트레이션")
class SfClaimResendBatchServiceTest {

    private val claimRepository: ClaimRepository = mockk()
    private val claimSfDispatchService: ClaimSfDispatchService = mockk()
    private val suggestionRepository: SuggestionRepository = mockk()
    private val suggestionSfResendService: SuggestionSfResendService = mockk()

    private val maxAttempt = 3

    private fun service() = SfClaimResendBatchService(
        claimRepository, claimSfDispatchService, suggestionRepository, suggestionSfResendService, maxAttempt,
    )

    private fun claimDispatchResult(sfSendStatus: ClaimSfSendStatus) =
        ClaimSfDispatchService.DispatchResult(sfResult = mockk(relaxed = true), sfSendStatus = sfSendStatus)

    @Test
    fun `대상 조회는 SEND_FAILED + maxAttempt 상한으로 호출`() {
        every { claimRepository.findResendTargetIds(ClaimSfSendStatus.SEND_FAILED, maxAttempt) } returns emptyList()
        every { suggestionRepository.findResendTargetIds(SuggestionSfSendStatus.SEND_FAILED, maxAttempt) } returns emptyList()

        service().resendAll()

        verify { claimRepository.findResendTargetIds(ClaimSfSendStatus.SEND_FAILED, maxAttempt) }
        verify { suggestionRepository.findResendTargetIds(SuggestionSfSendStatus.SEND_FAILED, maxAttempt) }
    }

    @Test
    fun `두 도메인 재전송 후 metadata 에 도메인별 집계 기록`() {
        every { claimRepository.findResendTargetIds(any(), any()) } returns listOf(1L, 2L)
        every { claimSfDispatchService.dispatch(1L, any(), any()) } returns claimDispatchResult(ClaimSfSendStatus.SENT)
        every { claimSfDispatchService.dispatch(2L, any(), any()) } returns claimDispatchResult(ClaimSfSendStatus.SEND_FAILED)

        every { suggestionRepository.findResendTargetIds(any(), any()) } returns listOf(10L)
        every { suggestionSfResendService.resend(10L) } returns
            SuggestionSfResendService.SfPushResult(success = true, apiResponse = null, errorSummary = null)

        val ctx = ScheduledJobRunContext("sf-claim-resend")
        service().resendAll(ctx)

        val meta = ctx.pendingMetadata()!!
        @Suppress("UNCHECKED_CAST")
        val claim = meta["claim"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val suggestion = meta["suggestion"] as Map<String, Any?>

        assertThat(claim["target"]).isEqualTo(2)
        assertThat(claim["sent"]).isEqualTo(1)
        assertThat(claim["failed"]).isEqualTo(1)
        assertThat(suggestion["target"]).isEqualTo(1)
        assertThat(suggestion["sent"]).isEqualTo(1)
        assertThat(suggestion["failed"]).isEqualTo(0)
    }

    @Test
    fun `제품클레임 조회 단계 예외가 물류클레임 처리를 막지 않는다 (도메인 격리)`() {
        every { claimRepository.findResendTargetIds(any(), any()) } throws RuntimeException("DB 장애")
        every { suggestionRepository.findResendTargetIds(any(), any()) } returns listOf(10L)
        every { suggestionSfResendService.resend(10L) } returns
            SuggestionSfResendService.SfPushResult(success = true, apiResponse = null, errorSummary = null)

        val ctx = ScheduledJobRunContext("sf-claim-resend")
        service().resendAll(ctx)

        // 물류클레임은 정상 처리됨.
        verify { suggestionSfResendService.resend(10L) }
        val meta = ctx.pendingMetadata()!!
        @Suppress("UNCHECKED_CAST")
        val claim = meta["claim"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val suggestion = meta["suggestion"] as Map<String, Any?>
        assertThat(claim["error"]).isEqualTo(true)
        assertThat(suggestion["sent"]).isEqualTo(1)
    }

    @Test
    fun `상태 가드 mismatch(dispatch null)는 failed 가 아니라 skipped 로 집계`() {
        // 배치 조회 후 다른 경로가 이미 SENT 로 전이 → dispatch 가 null 반환.
        every { claimRepository.findResendTargetIds(any(), any()) } returns listOf(1L)
        every { claimSfDispatchService.dispatch(1L, any(), any()) } returns null
        every { suggestionRepository.findResendTargetIds(any(), any()) } returns listOf(10L)
        every { suggestionSfResendService.resend(10L) } returns null

        val ctx = ScheduledJobRunContext("sf-claim-resend")
        service().resendAll(ctx)

        val meta = ctx.pendingMetadata()!!
        @Suppress("UNCHECKED_CAST")
        val claim = meta["claim"] as Map<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val suggestion = meta["suggestion"] as Map<String, Any?>
        assertThat(claim["skipped"]).isEqualTo(1)
        assertThat(claim["failed"]).isEqualTo(0)
        assertThat(suggestion["skipped"]).isEqualTo(1)
        assertThat(suggestion["failed"]).isEqualTo(0)
    }

    @Test
    fun `건별 dispatch 예외는 격리되어 failed 로 집계되고 다음 건 진행`() {
        every { claimRepository.findResendTargetIds(any(), any()) } returns listOf(1L, 2L)
        every { claimSfDispatchService.dispatch(1L, any(), any()) } throws RuntimeException("일시 오류")
        every { claimSfDispatchService.dispatch(2L, any(), any()) } returns claimDispatchResult(ClaimSfSendStatus.SENT)
        every { suggestionRepository.findResendTargetIds(any(), any()) } returns emptyList()

        val ctx = ScheduledJobRunContext("sf-claim-resend")
        service().resendAll(ctx)

        verify { claimSfDispatchService.dispatch(2L, any(), any()) }
        val meta = ctx.pendingMetadata()!!
        @Suppress("UNCHECKED_CAST")
        val claim = meta["claim"] as Map<String, Any?>
        assertThat(claim["target"]).isEqualTo(2)
        assertThat(claim["sent"]).isEqualTo(1)
        assertThat(claim["failed"]).isEqualTo(1)
    }
}
