package com.otoki.powersales.account.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 거래처(Account) 도메인 예외 모음. (Spec #640, #642)
 *
 * - #640: 관리자 웹 신규 거래처 등록(`POST /api/v1/admin/accounts`) 흐름의 검증 실패 케이스
 * - #642: 관리자 웹 거래처 삭제(`DELETE /api/v1/admin/accounts/{id}`) 흐름의 차단/조회 실패 케이스
 */
class AccountNameBlankException : BusinessException(
    errorCode = "ACCOUNT_NAME_BLANK",
    message = "거래처명은 필수입니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AccountNotFoundException : BusinessException(
    errorCode = "ACCOUNT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다.",
    httpStatus = HttpStatus.NOT_FOUND
)

class AccountDeleteBlockedSapSyncedException : BusinessException(
    errorCode = "ACCOUNT_DELETE_BLOCKED_SAP_SYNCED",
    message = "거래처 코드가 있는 거래처는 삭제할 수 없습니다.",
    httpStatus = HttpStatus.CONFLICT
)

class AccountNamePrefixRequiredException(allowedPrefixList: String) : BusinessException(
    errorCode = "ACCOUNT_NAME_PREFIX_REQUIRED",
    message = "신규 거래처 등록은 ($allowedPrefixList) 중 1개를 필수로 입력하셔야 합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AccountNameDuplicateException : BusinessException(
    errorCode = "ACCOUNT_NAME_DUPLICATE",
    message = "동일한 이름의 거래처가 이미 존재합니다.",
    httpStatus = HttpStatus.CONFLICT
)

class EmployeeCodeBlankException : BusinessException(
    errorCode = "EMPLOYEE_CODE_BLANK",
    message = "담당 영업사원 사번은 필수입니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class EmployeeNotFoundException(employeeCode: String) : BusinessException(
    errorCode = "EMPLOYEE_NOT_FOUND",
    message = "담당 영업사원을 찾을 수 없습니다: $employeeCode",
    httpStatus = HttpStatus.NOT_FOUND
)
