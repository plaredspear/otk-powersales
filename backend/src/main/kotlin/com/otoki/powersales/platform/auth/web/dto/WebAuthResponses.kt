package com.otoki.powersales.platform.auth.web.dto

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
 *
 * `role`: SF DKRetail__AppAuthority__c picklist value (`여사원` / `조장` / `지점장` / `AccountViewAll`) 또는 null.
 * SF picklist value 자체가 한글 label 이라 별도 roleLabel 필드 부재 (spec #807).
 */
data class WebUserSummary(
    val userId: Long,
    val username: String,
    val name: String?,
    val employeeCode: String?,
    val profileName: String? = null,
    val isSalesSupport: Boolean,
    val role: String?,
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
 *
 * 변경 성공 시 새 토큰 페어를 함께 발급한다. 기존 access token 클레임에는 여전히
 * `password_change_required=true` 가 박혀 있어, 재로그인 없이 강제 변경을 끝내려면
 * 클레임이 `false` 로 갱신된 새 토큰이 필요하다 (모바일 [com.otoki.powersales.platform.auth.dto.response.ChangePasswordResponse] 정합).
 * 대행(impersonation) 중에는 비밀번호 변경 자체가 차단되므로 토큰 재발급 경로도 대행과 무관하다.
 */
data class WebChangePasswordResponse(
    val passwordChangeRequired: Boolean,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
