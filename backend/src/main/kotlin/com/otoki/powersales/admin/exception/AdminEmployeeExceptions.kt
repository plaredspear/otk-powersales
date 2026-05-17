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

/**
 * 사원 단건 조회 실패 — 존재하지 않는 employeeId.
 */
class EmployeeNotFoundException(employeeId: Long) : BusinessException(
    errorCode = "EMPLOYEE_NOT_FOUND",
    message = "사원을 찾을 수 없습니다 (id=$employeeId)",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * SAP 가 원천인 사원 (origin=SAP) 의 web admin 수동 수정 차단.
 *
 * 레거시 IF_REST_SAP_EmployeeMaster 는 SAP 가 사원 마스터의 single source of truth 이며
 * 수동 수정과 SAP 인입이 경합하면 SAP 가 덮어쓴다. 본 예외는 수정 시도 자체를 사전 차단한다.
 */
class SapOriginEmployeeNotEditableException(employeeCode: String) : BusinessException(
    errorCode = "SAP_ORIGIN_NOT_EDITABLE",
    message = "SAP 가 원천인 사원은 web admin 에서 수정할 수 없습니다 (사번=$employeeCode). SAP 인입을 통해서만 갱신됩니다.",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 전문행사조 허용값 검증 실패.
 *
 * 레거시 EmployeeTriggerHandler — 직무코드 판촉직·레이디직·OSC직 시 전문행사조 값이 허용 목록 외이면 저장 차단.
 */
class InvalidProfessionalPromotionTeamException(value: String) : BusinessException(
    errorCode = "INVALID_PROFESSIONAL_PROMOTION_TEAM",
    message = "전문행사조에 들어갈 수 없는 값이 입력되어 있습니다. 오타 등을 확인하시기 바랍니다 (입력값=$value)",
    httpStatus = HttpStatus.BAD_REQUEST
)
