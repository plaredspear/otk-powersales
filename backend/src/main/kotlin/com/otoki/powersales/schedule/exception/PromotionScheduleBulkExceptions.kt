package com.otoki.powersales.schedule.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

// ===== Spec #571 P1-B: 행사 단위 일정 일괄 변경 =====

class PromotionScheduleBulkInvalidSizeException : BusinessException(
    errorCode = "BAD_REQUEST",
    message = "items는 1~500건이어야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PromotionScheduleBulkDeleteInvalidSizeException : BusinessException(
    errorCode = "BAD_REQUEST",
    message = "schedule_ids는 1~500건이어야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PromotionScheduleBulkDuplicateException : BusinessException(
    errorCode = "DUPLICATE_IN_REQUEST",
    message = "요청 내 동일 사원·동일 일자가 중복됩니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PromotionScheduleWorkingDateOutOfPromotionException(date: java.time.LocalDate) : BusinessException(
    errorCode = "WORKING_DATE_OUT_OF_PROMOTION",
    message = "행사 기간을 벗어난 일자입니다: $date",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PromotionScheduleInvalidWorkingCategoryException(value: String) : BusinessException(
    errorCode = "INVALID_WORKING_CATEGORY",
    message = "유효하지 않은 카테고리: $value",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PromotionScheduleNotInPromotionException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "다른 행사의 일정은 변경할 수 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 일괄 삭제 요청 schedule_ids 중 일부가 존재하지 않을 때 사용한다.
 * 응답에 `missing_ids` 배열을 포함하기 위해 GlobalExceptionHandler 가 별도 분기로 처리한다.
 */
class PromotionScheduleNotFoundPartialException(
    val missingIds: List<Long>
) : BusinessException(
    errorCode = "SCHEDULE_NOT_FOUND_PARTIAL",
    message = "일부 일정을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
