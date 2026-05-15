package com.otoki.powersales.admin.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * web admin User 관리 화면에서 사용하는 도메인 예외.
 */

class AdminUserNotFoundException(userId: Long) : BusinessException(
    errorCode = "USER_NOT_FOUND",
    message = "User($userId) 를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 자기 자신을 비활성화하려고 시도한 경우 (운영자 잠금 회피용 가드).
 */
class CannotDeactivateSelfException : BusinessException(
    errorCode = "CANNOT_DEACTIVATE_SELF",
    message = "자기 자신 계정은 비활성화할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
