package com.otoki.powersales.admin.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 시스템 관리자 수동 등록 도메인 예외 (Spec #579).
 *
 * errorCode 명명은 현행 컨벤션(prefix 없는 SCREAMING_SNAKE)을 따른다.
 */

class InvalidEmployeeCodeFormatException : BusinessException(
    errorCode = "INVALID_EMPLOYEE_CODE_FORMAT",
    message = "사번은 'ADMIN-' 으로 시작해야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PasswordConfirmMismatchException : BusinessException(
    errorCode = "PASSWORD_CONFIRM_MISMATCH",
    message = "비밀번호와 비밀번호 확인이 일치하지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AdminPasswordPolicyViolationException(detail: String) : BusinessException(
    errorCode = "PASSWORD_POLICY_VIOLATION",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class EmployeeCodeDuplicatedException : BusinessException(
    errorCode = "EMPLOYEE_CODE_DUPLICATED",
    message = "이미 사용 중인 사번입니다",
    httpStatus = HttpStatus.CONFLICT
)
