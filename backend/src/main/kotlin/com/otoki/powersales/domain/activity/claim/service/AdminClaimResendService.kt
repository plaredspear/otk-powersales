package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.dto.response.AdminClaimCreateResponse
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotResendableException
import org.springframework.stereotype.Service

/**
 * SF 재전송 service — Spec #829.
 *
 * 사전 조건: claim.status == SEND_FAILED (그 외 상태는 409).
 * 처리: S3 이미지 회수 → SF push → status update. 실제 전송 골격은 [ClaimSfDispatchService] 에 위임하며,
 * 신규 등록 직후 비동기 전송([ClaimSfPushDispatcher])과 동일 경로를 공유한다.
 */
@Service
class AdminClaimResendService(
    private val dispatchService: ClaimSfDispatchService,
) {

    fun resend(claimId: Long): AdminClaimCreateResponse {
        val result = dispatchService.dispatch(
            claimId = claimId,
            allowedStatuses = setOf(ClaimStatus.SEND_FAILED),
            onStatusMismatch = { throw ClaimNotResendableException() },
        ) ?: throw ClaimNotResendableException()

        val sfResult = result.sfResult
        return AdminClaimCreateResponse(
            claimId = claimId,
            status = result.status?.name ?: "",
            sfResultCode = sfResult.apiResponse?.resultCode,
            sfResultMsg = sfResult.apiResponse?.resultMsg ?: sfResult.errorSummary,
        )
    }
}
