package com.otoki.powersales.admin.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class InvalidYearMonthException : BusinessException(
    errorCode = "VALIDATION_ERROR",
    message = "유효하지 않은 yearMonth 형식입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
