package com.otoki.internal.admin.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class InvalidDateFormatException : BusinessException(
    errorCode = "INVALID_DATE_FORMAT",
    message = "날짜 형식이 올바르지 않습니다 (YYYY-MM-DD)",
    httpStatus = HttpStatus.BAD_REQUEST
)
