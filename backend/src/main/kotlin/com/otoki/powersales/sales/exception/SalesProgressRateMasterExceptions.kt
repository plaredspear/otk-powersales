package com.otoki.powersales.sales.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class SalesProgressRateMasterNotFoundException : BusinessException(
    errorCode = "SALES_PROGRESS_RATE_MASTER_NOT_FOUND",
    message = "거래처목표등록마스터를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
