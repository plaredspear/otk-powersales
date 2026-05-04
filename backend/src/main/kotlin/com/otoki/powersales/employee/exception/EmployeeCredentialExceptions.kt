package com.otoki.powersales.employee.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 사원 자격 정보(단말 UUID / 비밀번호) 운영자 리셋 도메인 예외 (Spec #582 P1-B).
 *
 * errorCode 명명은 #576 컨벤션(UPPER_SNAKE_CASE + 도메인 prefix)에 따라 `EMP_*` 를 사용한다.
 */

class EmployeeNotFoundException(employeeId: Long) : BusinessException(
    errorCode = "EMP_NOT_FOUND",
    message = "사원을 찾을 수 없습니다: $employeeId",
    httpStatus = HttpStatus.NOT_FOUND
)

class EmployeeCredentialForbiddenException : BusinessException(
    errorCode = "EMP_AUTH_FORBIDDEN",
    message = "사원 자격 정보 리셋 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class EmployeeLoginInactiveException : BusinessException(
    errorCode = "EMP_LOGIN_INACTIVE",
    message = "앱 로그인이 활성화된 사원만 자격 정보를 리셋할 수 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
