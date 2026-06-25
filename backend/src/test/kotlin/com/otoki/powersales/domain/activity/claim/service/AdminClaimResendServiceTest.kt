package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotResendableException
import com.otoki.powersales.external.sf.outbound.SfApiResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * AdminClaimResendService 테스트 — SF 재전송 (Spec #829).
 *
 * 실제 전송 골격은 [ClaimSfDispatchService] 가 담당하므로 (검증은 ClaimSfDispatchServiceTest),
 * 본 테스트는 SEND_FAILED 만 허용하는 가드 전달 + 응답 매핑 + skip 시 409 변환만 검증한다.
 */
@DisplayName("AdminClaimResendService 테스트 — SF 재전송 (Spec #829)")
class AdminClaimResendServiceTest {

    private val dispatchService: ClaimSfDispatchService = mockk()
    private val service = AdminClaimResendService(dispatchService)

    private val claimId = 42L

    @Test
    @DisplayName("SEND_FAILED 만 허용하는 가드 전달 + SF 결과를 응답에 매핑")
    fun resendDelegatesWithSendFailedGuard() {
        val allowedSlot = slot<Set<ClaimStatus>>()
        every {
            dispatchService.dispatch(claimId, capture(allowedSlot), any())
        } returns ClaimSfDispatchService.DispatchResult(
            sfResult = ClaimSfOutboundService.SfPushResult(
                success = true,
                apiResponse = SfApiResponse(resultCode = "200", resultMsg = "OK", rawBody = "{}"),
                errorSummary = null,
            ),
            status = ClaimStatus.SENT,
        )

        val result = service.resend(claimId)

        // 재전송은 SEND_FAILED 상태만 허용
        assertThat(allowedSlot.captured).containsExactly(ClaimStatus.SEND_FAILED)
        assertThat(result.claimId).isEqualTo(claimId)
        assertThat(result.status).isEqualTo("SENT")
        assertThat(result.sfResultCode).isEqualTo("200")
        assertThat(result.sfResultMsg).isEqualTo("OK")
    }

    @Test
    @DisplayName("dispatch skip(null) → ClaimNotResendableException 409")
    fun resendThrowsWhenDispatchSkips() {
        // 상태 불일치로 dispatch 가 onStatusMismatch 호출 후 null 반환하는 경우.
        every { dispatchService.dispatch(claimId, any(), any()) } answers {
            thirdArg<(ClaimStatus?) -> Unit>().invoke(ClaimStatus.SENT)
            null
        }

        assertThatThrownBy { service.resend(claimId) }
            .isInstanceOf(ClaimNotResendableException::class.java)
    }

    @Test
    @DisplayName("dispatch 내부 가드가 직접 throw 해도 그대로 전파")
    fun resendPropagatesGuardException() {
        every { dispatchService.dispatch(claimId, any(), any()) } throws ClaimNotResendableException()

        assertThatThrownBy { service.resend(claimId) }
            .isInstanceOf(ClaimNotResendableException::class.java)
        verify { dispatchService.dispatch(claimId, any(), any()) }
    }
}
