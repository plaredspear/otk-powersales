package com.otoki.powersales.account.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 거래처(Account) 도메인 예외 모음. (Spec #640, #642, #643)
 *
 * - #640: 관리자 웹 신규 거래처 등록(`POST /api/v1/admin/accounts`) 흐름의 검증 실패 케이스
 * - #642: 관리자 웹 거래처 삭제(`DELETE /api/v1/admin/accounts/{id}`) 흐름의 차단/조회 실패 케이스
 * - #643: 관리자 웹 거래처 수정(`PUT /api/v1/admin/accounts/{id}`) 흐름의 검증 실패 / 조회 실패 / prefix 위반 케이스
 */
class AccountNameBlankException : BusinessException(
    errorCode = "ACCOUNT_NAME_BLANK",
    message = "거래처명은 필수입니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 거래처 미존재 / soft-delete 처리된 거래처 조회 예외.
 *
 * - #642 (delete): id 미명시 호출 — `AccountNotFoundException()` → 메시지 "거래처를 찾을 수 없습니다."
 * - #643 (update): id 명시 호출 — `AccountNotFoundException(id)` → 메시지 "거래처를 찾을 수 없습니다: {id}"
 *
 * errorCode 는 `ACCOUNT_NOT_FOUND` 동일 — id 노출은 메시지 디테일만 분기.
 */
class AccountNotFoundException(id: Int? = null) : BusinessException(
    errorCode = "ACCOUNT_NOT_FOUND",
    message = if (id != null) "거래처를 찾을 수 없습니다: $id" else "거래처를 찾을 수 없습니다.",
    httpStatus = HttpStatus.NOT_FOUND
)

class AccountDeleteBlockedSapSyncedException : BusinessException(
    errorCode = "ACCOUNT_DELETE_BLOCKED_SAP_SYNCED",
    message = "거래처 코드가 있는 거래처는 삭제할 수 없습니다.",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 거래처 소유자(owner)가 아닌 사용자의 삭제 시도 차단 예외 (#642 — SF owner 가드 동등).
 *
 * 레거시 SF `AccountTriggerHandler.AccountDeleteCheck` 의
 * `OwnerId != UserInfo.getUserId()` → `addError('OwnerId', '자신의 신규 거래처만 삭제가 가능합니다.')`
 * 와 동등. SF 는 삭제 trigger 에 Profile 예외가 없어 시스템 관리자도 동일하게 적용되므로,
 * 신규도 권한 키와 무관하게 owner 본인만 native 거래처를 삭제할 수 있다.
 */
class AccountDeleteNotOwnerException : BusinessException(
    errorCode = "ACCOUNT_DELETE_NOT_OWNER",
    message = "자신의 신규 거래처만 삭제가 가능합니다.",
    httpStatus = HttpStatus.FORBIDDEN
)

class AccountNamePrefixRequiredException(allowedPrefixList: String) : BusinessException(
    errorCode = "ACCOUNT_NAME_PREFIX_REQUIRED",
    message = "신규 거래처 등록은 ($allowedPrefixList) 중 1개를 필수로 입력하셔야 합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 거래처 수정 시 prefix 화이트리스트 위반 예외 (#643).
 *
 * #640 의 [AccountNamePrefixRequiredException] 와 errorCode 는 동일하나, 사용자 안내 메시지를
 * "거래처 수정은 ..." 으로 분기 (등록/수정 흐름 명시). 기존 #640 호출부 영향 0.
 */
class AccountNamePrefixRequiredForUpdateException(allowedPrefixList: String) : BusinessException(
    errorCode = "ACCOUNT_NAME_PREFIX_REQUIRED",
    message = "거래처 수정은 ($allowedPrefixList) 중 1개를 필수로 입력하셔야 합니다.",
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
