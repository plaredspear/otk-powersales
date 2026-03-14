package com.otoki.internal.admin.exception

import com.otoki.internal.common.exception.BusinessException
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
