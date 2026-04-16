package com.otoki.internal.admin.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class AdminForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "시스템관리자만 접근할 수 있습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class InvalidPermissionException(permission: String) : BusinessException(
    errorCode = "INVALID_PERMISSION",
    message = "유효하지 않은 권한입니다: $permission",
    httpStatus = HttpStatus.BAD_REQUEST
)

class DuplicatePermissionException(permission: String) : BusinessException(
    errorCode = "DUPLICATE_PERMISSION",
    message = "중복된 권한이 포함되어 있습니다: $permission",
    httpStatus = HttpStatus.BAD_REQUEST
)

class CannotModifyOwnPermissionException : BusinessException(
    errorCode = "CANNOT_MODIFY_OWN_PERMISSION",
    message = "자기 자신의 권한은 수정할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class CannotModifyOwnAuthorityException : BusinessException(
    errorCode = "CANNOT_MODIFY_OWN_AUTHORITY",
    message = "자기 자신의 역할은 변경할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidAuthorityException(authority: String) : BusinessException(
    errorCode = "INVALID_AUTHORITY",
    message = "유효하지 않은 역할입니다: $authority",
    httpStatus = HttpStatus.BAD_REQUEST
)
