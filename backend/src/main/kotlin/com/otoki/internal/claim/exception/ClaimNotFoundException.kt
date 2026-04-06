package com.otoki.internal.claim.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class ClaimNotFoundException(claimId: Long) : BusinessException(
    errorCode = "CLAIM_NOT_FOUND",
    message = "클레임을 찾을 수 없습니다: $claimId",
    httpStatus = HttpStatus.NOT_FOUND
)
