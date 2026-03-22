package com.otoki.internal.promotion.exception

import com.otoki.internal.common.exception.BusinessException
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
