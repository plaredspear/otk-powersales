package com.otoki.internal.promotion.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class PromotionNotFoundException : BusinessException(
    errorCode = "PROMOTION_NOT_FOUND",
    message = "행사마스터를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class InvalidDateRangeException : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = "종료일이 시작일보다 이전입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class CostCenterNotFoundException : BusinessException(
    errorCode = "COST_CENTER_NOT_FOUND",
    message = "생성자의 지점코드가 존재하지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AccountNotFoundException : BusinessException(
    errorCode = "ACCOUNT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class ProductNotFoundException : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = "상품을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class PromotionForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class PromotionInvalidParameterException : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = "유효하지 않은 파라미터",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PromotionTypeDuplicateException : BusinessException(
    errorCode = "PROMOTION_TYPE_DUPLICATE",
    message = "이미 존재하는 행사유형입니다",
    httpStatus = HttpStatus.CONFLICT
)

class PromotionTypeNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "행사유형을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class InvalidPromotionTypeException : BusinessException(
    errorCode = "INVALID_PROMOTION_TYPE",
    message = "유효하지 않은 행사유형입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidStandLocationException : BusinessException(
    errorCode = "INVALID_STAND_LOCATION",
    message = "유효하지 않은 매대위치입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class PromotionEmployeeNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "행사조원을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class InvalidWorkStatusException : BusinessException(
    errorCode = "INVALID_WORK_STATUS",
    message = "근무상태는 근무, 연차, 대휴 중 하나여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidWorkType3Exception : BusinessException(
    errorCode = "INVALID_WORK_TYPE3",
    message = "근무유형3은 고정, 격고, 순회 중 하나여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class TeamCategoryMismatchException(detail: String) : BusinessException(
    errorCode = "TEAM_CATEGORY_MISMATCH",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class ClosedEmployeeModificationException : BusinessException(
    errorCode = "CLOSED_EMPLOYEE_MODIFICATION",
    message = "확정되었고 여사원이 마감한 행사조원은 수정할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ClosedEmployeeDeleteException : BusinessException(
    errorCode = "CLOSED_EMPLOYEE_DELETE",
    message = "확정되었고 여사원이 마감한 행사조원은 삭제할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ClosedPromotionModificationException : BusinessException(
    errorCode = "CLOSED_PROMOTION_MODIFICATION",
    message = "일마감이 하나라도 등록된 행사의 시작일, 종료일 및 거래처는 변경할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ClosedPromotionDeleteException : BusinessException(
    errorCode = "CLOSED_PROMOTION_DELETE",
    message = "일마감이 하나라도 등록된 행사는 삭제할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class DateRangeConflictException(minDate: String, maxDate: String) : BusinessException(
    errorCode = "DATE_RANGE_CONFLICT",
    message = "행사 일정은 행사조원 할당일 범위(${minDate} ~ ${maxDate})보다 작을 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

// --- 행사 확정 검증 예외 (#191) ---

class PromotionDateRequiredException : BusinessException(
    errorCode = "DATE_REQUIRED",
    message = "행사마스터의 시작일과 종료일을 입력하세요",
    httpStatus = HttpStatus.BAD_REQUEST
)

class NoEmployeesException : BusinessException(
    errorCode = "NO_EMPLOYEES",
    message = "확정할 행사조원이 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ValuesRequiredException(detail: String) : BusinessException(
    errorCode = "VALUES_REQUIRED",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class DateOutOfRangeException(detail: String) : BusinessException(
    errorCode = "DATE_OUT_OF_RANGE",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class WorkType3LimitExceededException(detail: String) : BusinessException(
    errorCode = "WORK_TYPE3_LIMIT_EXCEEDED",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class LeaveConflictException(detail: String) : BusinessException(
    errorCode = "LEAVE_CONFLICT",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class DuplicateScheduleException(detail: String) : BusinessException(
    errorCode = "DUPLICATE_SCHEDULE",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class EmployeeOnLeaveException(detail: String) : BusinessException(
    errorCode = "EMPLOYEE_ON_LEAVE",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class EmployeeResignedException(detail: String) : BusinessException(
    errorCode = "EMPLOYEE_RESIGNED",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidOtherProductException : BusinessException(
    errorCode = "INVALID_OTHER_PRODUCT",
    message = "기타상품에 작은따옴표(')를 사용할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ScheduleDateOutOfRangeException(scheduleDate: String, startDate: String, endDate: String) : BusinessException(
    errorCode = "SCHEDULE_DATE_OUT_OF_RANGE",
    message = "투입일(${scheduleDate})이 행사 기간(${startDate} ~ ${endDate})을 벗어납니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
