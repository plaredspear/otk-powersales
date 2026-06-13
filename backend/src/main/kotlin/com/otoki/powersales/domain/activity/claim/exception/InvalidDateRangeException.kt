package com.otoki.powersales.domain.activity.claim.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class InvalidDateRangeException : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = "시작일이 종료일보다 미래일 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
