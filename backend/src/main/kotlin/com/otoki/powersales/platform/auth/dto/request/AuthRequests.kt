package com.otoki.powersales.platform.auth.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 로그인 요청 DTO
 *
 * 사번 형식: Heroku 레거시 로그인과 동일하게 형식 검증을 두지 않는다 (8자리/숫자 전용 제약 없음).
 * SF `DKRetail__EmpCode__c` 가 string(100) 이라 길이 상한 100자만 안전장치로 유지.
 * 인증 성공 여부는 입력 사번이 저장된 사번과 정확히 일치하는지로 결정.
 *
 * deviceId: native app 전용 엔드포인트이므로 필수. 모바일 클라이언트가 단말기 식별자를
 * 항상 채워 전송한다 (조회 실패 시에도 fallback 으로 non-empty 보장).
 */
data class LoginRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    @field:Size(max = 100, message = "사번은 100자를 초과할 수 없습니다")
    val employeeCode: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 4, message = "비밀번호는 4글자 이상이어야 합니다")
    val password: String,

    @field:NotBlank(message = "단말기 식별자는 필수입니다")
    val deviceId: String,

    // 자동로그인 선택 여부. true 면 장수명(60일) refresh token 을 발급해 오래 방치해도 세션이
    // 유지된다. false(기본 — 구버전 클라이언트 미전송 포함)면 기본 7일 롤링 세션.
    val autoLogin: Boolean = false,

    // 현재 사용 중인 앱 버전 보고 (선택 — 구버전 클라이언트는 미전송). 서버가 사용자별 현재 버전 기록용.
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val appPlatform: String? = null
)

/**
 * 비밀번호 변경 요청 DTO (강제/자발 통합).
 *
 * - 강제 변경 (토큰 `passwordChangeRequired=true`): `currentPassword` 무시 (전달되어도 미검증).
 * - 자발 변경: `currentPassword` 필수 — 누락 시 `AUTH_CURRENT_PASSWORD_REQUIRED`, 불일치 시 `AUTH_CURRENT_PASSWORD_MISMATCH`.
 *
 * 신규 비밀번호 정책 검증은 [com.otoki.powersales.platform.auth.policy.PasswordPolicyValidator] 가 권위.
 */
data class ChangePasswordRequest(
    val currentPassword: String? = null,

    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    val newPassword: String,

    // 자동로그인 선택 여부(로그인 화면 체크박스와 동일). 변경 시 새 refresh family 로 재발급되므로,
    // 클라이언트가 현재 선호를 전달해 ON 세션의 장수명(60일)을 유지한다. 기본 false = 7일 세션.
    val autoLogin: Boolean = false
)

/**
 * 토큰 갱신 요청 DTO
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh Token은 필수입니다")
    val refreshToken: String,

    // 현재 사용 중인 앱 버전 보고 (선택). 자동 로그인 중 주기적 리프레시로 현재 버전이 최신화된다.
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val appPlatform: String? = null
)

/**
 * 비밀번호 검증 요청 DTO (자발 변경 1단계).
 */
data class VerifyPasswordRequest(
    @field:NotBlank(message = "비밀번호를 입력해주세요")
    val currentPassword: String
)
