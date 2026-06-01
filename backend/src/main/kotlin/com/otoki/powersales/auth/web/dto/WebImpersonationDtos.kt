package com.otoki.powersales.auth.web.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

/**
 * 대행 시작 요청 (Spec #851 §1.1).
 */
data class WebImpersonationStartRequest(
    @field:NotNull(message = "대행 대상 사용자 ID는 필수입니다")
    val targetUserId: Long?,

    @field:Size(max = 500, message = "대행 사유는 500자 이내여야 합니다")
    val reason: String? = null,
)

/**
 * 대행 메타 — 누가 누구를 언제부터 대행 중인지 (Spec #851 §1.1).
 */
data class WebImpersonationMeta(
    val impersonatedByUserId: Long,
    val impersonatedByName: String?,
    val targetUserId: Long,
    val targetName: String?,
    val startedAt: LocalDateTime,
)

/**
 * 대행 시작 응답 (Spec #851 §1.1).
 *
 * `user` 는 대행 대상 사용자 기준 [WebUserSummary].
 */
data class WebImpersonationStartResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val impersonation: WebImpersonationMeta,
    val user: WebUserSummary,
)

/**
 * 대행 종료 응답 (Spec #851 §1.2).
 *
 * `user` 는 복귀한 관리자 기준 [WebUserSummary].
 */
data class WebImpersonationStopResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val user: WebUserSummary,
)
