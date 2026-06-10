package com.otoki.powersales.auth.exception

import com.otoki.powersales.common.exception.BusinessException

import org.springframework.http.HttpStatus

/**
 * 인증 실패 (사번/비밀번호 불일치)
 */
class InvalidCredentialsException : BusinessException(
    errorCode = "INVALID_CREDENTIALS",
    message = "사번 또는 비밀번호가 올바르지 않습니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * 현재 비밀번호 불일치 (자발 변경 / 본인 검증).
 */
class InvalidCurrentPasswordException : BusinessException(
    errorCode = "AUTH_CURRENT_PASSWORD_MISMATCH",
    message = "현재 비밀번호가 일치하지 않습니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * 자발 변경 시 현재 비밀번호 누락.
 */
class CurrentPasswordRequiredException : BusinessException(
    errorCode = "AUTH_CURRENT_PASSWORD_REQUIRED",
    message = "현재 비밀번호를 입력해주세요",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 새 비밀번호 정책 위반 (길이/반복).
 *
 * `violations` 는 위반 규칙 코드 배열 (예: ["LENGTH_TOO_SHORT", "REPEATED_CHARACTERS"]).
 * GlobalExceptionHandler 가 `error.details.violations` 로 노출한다.
 */
class NewPasswordPolicyViolationException(
    val violations: List<String>
) : BusinessException(
    errorCode = "AUTH_NEW_PASSWORD_INVALID",
    message = "비밀번호가 정책을 충족하지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 강제 변경 미완료 사원이 화이트리스트 외 API 호출.
 *
 * 화이트리스트: change-password / logout / refresh-token.
 */
class PasswordChangeRequiredException : BusinessException(
    errorCode = "AUTH_PASSWORD_CHANGE_REQUIRED",
    message = "비밀번호를 변경해주세요. 임시 비밀번호 상태에서는 다른 기능을 사용할 수 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 유효하지 않은 토큰
 */
class InvalidTokenException : BusinessException(
    errorCode = "INVALID_TOKEN",
    message = "유효하지 않은 Refresh Token입니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * 사원 없음
 */
class EmployeeNotFoundException : BusinessException(
    errorCode = "USER_NOT_FOUND",
    message = "사용자를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 단말기 불일치
 */
class DeviceMismatchException : BusinessException(
    errorCode = "DEVICE_MISMATCH",
    message = "등록된 단말기와 다른 기기입니다. 관리자에게 문의하세요",
    httpStatus = HttpStatus.FORBIDDEN
)

class TermsNotFoundException : BusinessException(
    errorCode = "TERMS_NOT_FOUND",
    message = "활성화된 약관이 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class GpsConsentRequiredException : BusinessException(
    errorCode = "GPS_CONSENT_REQUIRED",
    message = "GPS 사용 동의가 필요합니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * Refresh Token 재사용 감지 (탈취 의심)
 */
class TokenReuseDetectedException : BusinessException(
    errorCode = "TOKEN_REUSE_DETECTED",
    message = "보안 이상이 감지되었습니다. 다시 로그인해주세요",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * 앱 로그인 비활성 (Mobile)
 */
class AppLoginNotActiveException : BusinessException(
    errorCode = "APP_LOGIN_NOT_ACTIVE",
    message = "앱 로그인이 비활성화되어 있습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 비활성 사용자 로그인 시도 (Spec #760 — User.is_active == false).
 */
class UserInactiveException : BusinessException(
    errorCode = "USER_INACTIVE",
    message = "비활성화된 계정입니다. 관리자에게 문의하세요",
    httpStatus = HttpStatus.FORBIDDEN
)

// ===== 대행 로그인 (Impersonation, Spec #851) =====
//
// 권한 미보유 거부는 별도 예외 없이 @RequiresSfPermission 표준 가드(WebAdminContextFilter)가
// PERMISSION_DENIED(403) 로 처리한다.

/**
 * 이미 대행 중인 토큰으로 대행 재시작 시도 (중첩 대행 금지).
 */
class ImpersonationAlreadyActiveException : BusinessException(
    errorCode = "IMPERSONATION_ALREADY_ACTIVE",
    message = "이미 다른 사용자를 대행 중입니다. 먼저 대행을 종료해주세요",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 대행 대상 User 없음.
 */
class ImpersonationTargetNotFoundException : BusinessException(
    errorCode = "IMPERSONATION_TARGET_NOT_FOUND",
    message = "대행 대상 사용자를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 대행 대상이 비활성 계정 (is_active == false).
 */
class ImpersonationTargetInactiveException : BusinessException(
    errorCode = "IMPERSONATION_TARGET_INACTIVE",
    message = "비활성화된 사용자는 대행할 수 없습니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 자기 자신을 대행 대상으로 지정.
 */
class ImpersonationSelfNotAllowedException : BusinessException(
    errorCode = "IMPERSONATION_SELF_NOT_ALLOWED",
    message = "본인 계정은 대행할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 대행 중이 아닌 토큰으로 대행 종료 시도.
 */
class ImpersonationNotActiveException : BusinessException(
    errorCode = "IMPERSONATION_NOT_ACTIVE",
    message = "대행 중인 세션이 아닙니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 대행 종료 시 복귀 대상 관리자 User 가 조회되지 않음 (삭제/소실).
 */
class ImpersonationAdminNotFoundException : BusinessException(
    errorCode = "IMPERSONATION_ADMIN_NOT_FOUND",
    message = "복귀할 관리자 계정을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 대행 중 대상 사용자 비밀번호 변경 시도 차단.
 */
class ImpersonationPasswordChangeBlockedException : BusinessException(
    errorCode = "IMPERSONATION_PASSWORD_CHANGE_BLOCKED",
    message = "대행 중에는 비밀번호를 변경할 수 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)
