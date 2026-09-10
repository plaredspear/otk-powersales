package com.otoki.powersales.domain.activity.promotion.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class PPTMasterNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "전문행사조 마스터를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class PPTMasterDuplicateException : BusinessException(
    errorCode = "CONFLICT",
    message = "중복으로 유효한 마스터가 존재합니다",
    httpStatus = HttpStatus.CONFLICT
)

class PPTMasterInvalidTeamTypeException : BusinessException(
    errorCode = "INVALID_TEAM_TYPE",
    message = "유효하지 않은 전문행사조 유형입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PPTMasterInvalidDateRangeException : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = "종료일이 시작일보다 이전입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PPTMasterEmployeeNotFoundException : BusinessException(
    errorCode = "EMPLOYEE_NOT_FOUND",
    message = "사원을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class PPTMasterAccountNotFoundException : BusinessException(
    errorCode = "ACCOUNT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class PPTMasterBulkLimitExceededException : BusinessException(
    errorCode = "BULK_LIMIT_EXCEEDED",
    message = "업로드 항목은 최대 450건까지 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PPTMasterBulkValidationFailedException : BusinessException(
    errorCode = "BULK_VALIDATION_FAILED",
    message = "검증 실패 항목이 존재합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 사원에 반영된 마스터의 삭제 차단.
 *
 * 삭제하면 사원 전문행사조가 근거를 잃고 남는데, 해제 배치는 `종료일 = 오늘` 인 마스터만 스캔하므로
 * 그 사원은 영영 해제되지 않는다 (행사조원 등록이 대표제품 매칭 검증에서 막힘).
 */
class PPTMasterAppliedDeleteForbiddenException : BusinessException(
    errorCode = "APPLIED_DELETE_FORBIDDEN",
    message = "이미 사원에 반영된 마스터는 삭제할 수 없습니다. 종료일을 지정해 종료해 주세요",
    httpStatus = HttpStatus.CONFLICT
)

/** 사원에 반영된 마스터의 핵심 필드(사원 / 전문행사조 / 시작일) 변경 차단 — 삭제 차단과 동일한 사유. */
class PPTMasterAppliedCoreFieldUpdateForbiddenException : BusinessException(
    errorCode = "APPLIED_UPDATE_FORBIDDEN",
    message = "이미 사원에 반영된 마스터는 사원 / 전문행사조 / 시작일을 변경할 수 없습니다. 종료일만 조정할 수 있습니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 종료일을 오늘 이전으로 지정하는 것을 차단.
 *
 * 해제 배치(`expireMasters`) 가 `종료일 = 오늘` 만 스캔하므로, 과거 날짜로 종료하면 어느 경로에도 걸리지 않아
 * 삭제와 동일하게 사원 값이 잔존한다.
 */
class PPTMasterEndDateInPastException : BusinessException(
    errorCode = "INVALID_END_DATE",
    message = "종료일은 오늘 이전으로 지정할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
