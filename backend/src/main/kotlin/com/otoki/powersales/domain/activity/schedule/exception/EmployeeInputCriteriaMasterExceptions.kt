package com.otoki.powersales.domain.activity.schedule.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class EmployeeInputCriteriaMasterNotFoundException : BusinessException(
    errorCode = "EMPLOYEE_INPUT_CRITERIA_NOT_FOUND",
    message = "진열사원 투입기준 마스터를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class EmployeeInputCriteriaDateRangeInvalidException : BusinessException(
    errorCode = "EMPLOYEE_INPUT_CRITERIA_DATE_RANGE_INVALID",
    message = "시작일, 종료일을 확인해주세요.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class EmployeeInputCriteriaPeriodOverlapException : BusinessException(
    errorCode = "EMPLOYEE_INPUT_CRITERIA_PERIOD_OVERLAP",
    message = "중복된 레코드가 존재합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class EmployeeInputCriteriaConfirmedEditDeniedException : BusinessException(
    errorCode = "EMPLOYEE_INPUT_CRITERIA_CONFIRMED_EDIT_DENIED",
    message = "확정 후에는 「종료일」이외는 편집할 수 없습니다. 시스템 관리자에게 문의하십시오.",
    httpStatus = HttpStatus.FORBIDDEN
)

class EmployeeInputCriteriaCategoryNotFoundException : BusinessException(
    errorCode = "EMPLOYEE_INPUT_CRITERIA_CATEGORY_NOT_FOUND",
    message = "지정한 거래처유형마스터를 찾을 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
