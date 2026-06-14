package com.otoki.powersales.schedule.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class ScheduleFileRequiredException : BusinessException(
    errorCode = "FILE_REQUIRED",
    message = "파일이 첨부되지 않았습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ScheduleInvalidFileTypeException : BusinessException(
    errorCode = "INVALID_FILE_TYPE",
    message = ".xlsx 파일만 업로드 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ScheduleEmptyFileException : BusinessException(
    errorCode = "EMPTY_FILE",
    message = "데이터 행이 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ScheduleRowLimitExceededException : BusinessException(
    errorCode = "ROW_LIMIT_EXCEEDED",
    message = "최대 500행까지 처리 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ScheduleFileTooLargeException : BusinessException(
    errorCode = "FILE_TOO_LARGE",
    message = "파일 크기는 5MB 이하여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ScheduleUploadNotFoundException : BusinessException(
    errorCode = "UPLOAD_NOT_FOUND",
    message = "업로드 검증 결과를 찾을 수 없습니다 (만료되었거나 존재하지 않음)",
    httpStatus = HttpStatus.NOT_FOUND
)

class ScheduleHasValidationErrorsException : BusinessException(
    errorCode = "HAS_VALIDATION_ERRORS",
    message = "검증 에러가 있는 상태에서는 확정할 수 없습니다",
    httpStatus = HttpStatus.CONFLICT
)

class ScheduleNotFoundException(message: String = "존재하지 않거나 삭제된 스케줄이 포함되어 있습니다") : BusinessException(
    errorCode = "SCHEDULE_NOT_FOUND",
    message = message,
    httpStatus = HttpStatus.NOT_FOUND
)

class ScheduleDeleteForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "지점장님은 스케줄 삭제 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class ScheduleDeleteConstraintException : BusinessException(
    errorCode = "SCHEDULE_DELETE_CONSTRAINT",
    message = "확정된 스케줄에 연결된 여사원 일정이 존재하여 삭제할 수 없습니다",
    httpStatus = HttpStatus.CONFLICT
)

class ScheduleValidationException(message: String) : BusinessException(
    errorCode = "SCHEDULE_VALIDATION_FAILED",
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * UC-05: 확정된 스케줄에서 조장이 종료일 외 필드를 변경 시도하면 차단.
 * 레거시 SF Validation Rule `EditDisableForDisplayMaster` 동등.
 */
class ScheduleEditBlockedAfterConfirmException : BusinessException(
    errorCode = "SCHEDULE_EDIT_BLOCKED_AFTER_CONFIRM",
    message = "확정 후에는 종료일 이외는 편집할 수 없습니다. 시스템 관리자에게 문의하십시오",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * UC-12 사업소 가시 범위 위반 — 조장이 본인 담당 사업소 외 레코드 접근 시도 시 차단.
 * 레거시 SF Sharing Rule 50+ 룰 (CostCenterCode 기준) 의 신규 매핑.
 */
class ScheduleForbiddenException : BusinessException(
    errorCode = "SCHEDULE_FORBIDDEN",
    message = "본인 담당 사업소 외 레코드는 접근할 수 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)
