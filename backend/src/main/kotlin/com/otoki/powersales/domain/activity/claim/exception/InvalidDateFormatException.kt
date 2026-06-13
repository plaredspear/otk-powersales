package com.otoki.powersales.domain.activity.claim.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class InvalidDateFormatException : BusinessException(
    errorCode = "INVALID_DATE_FORMAT",
    message = "날짜 형식이 올바르지 않습니다 (yyyy-MM-dd)",
    httpStatus = HttpStatus.BAD_REQUEST
)
