package com.otoki.powersales.admin.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * Admin 권한 거부 예외. 시스템 권한 비트 부재 시 service layer 에서 throw.
 *
 * Endpoint 가드 (`@RequiresSfPermission` + WebAdminContextFilter) 는 PERMISSION_DENIED 응답을 별도
 * 처리하므로 본 예외는 service 내부 SYSTEM_ADMIN role 강제 검사 등 보조적 검증에 사용된다.
 */
class AdminForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "시스템관리자만 접근할 수 있습니다",
    httpStatus = HttpStatus.FORBIDDEN
)
