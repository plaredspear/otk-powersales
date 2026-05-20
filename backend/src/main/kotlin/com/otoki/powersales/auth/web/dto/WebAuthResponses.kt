package com.otoki.powersales.auth.web.dto

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.user.entity.ProfileType

/**
 * Web 로그인 응답 (Spec #760 §5.1).
 */
data class WebLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val passwordChangeRequired: Boolean,
    val user: WebUserSummary
)

/**
 * Web 사용자 요약 — 로그인 응답에 포함되는 User 정보 (Spec #760).
 *
 * Web Admin UI 의 라우트 가드(`role`) / 권한 가드(`permissions`) / 데이터 스코프 분기
 * (`orgName`, `costCenterCode`) 를 본 응답 1회로 충족하도록 Employee + 권한 산출 결과를 함께 반환.
 * 동일 정보는 JWT access token claim 에도 실려서, 후속 API 요청 시 Filter 가 토큰만으로
 * principal 을 복원할 수 있다 (network round-trip 절감 + 권한 매트릭스 변경 시 재로그인 또는
 * access token 만료 후 반영).
 */
data class WebUserSummary(
    val userId: Long,
    val username: String,
    val name: String?,
    val employeeCode: String?,
    val profileType: ProfileType,
    val isSalesSupport: Boolean,
    val role: UserRole?,
    val roleLabel: String?,
    val orgName: String?,
    val costCenterCode: String?,
    val permissions: List<String>
)

/**
 * Web 토큰 갱신 응답 (Spec #760 §5.2).
 */
data class WebTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

/**
 * Web 비밀번호 변경 응답 (Spec #760 §5.3).
 */
data class WebChangePasswordResponse(
    val passwordChangeRequired: Boolean
)
