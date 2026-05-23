package com.otoki.powersales.admin.permission.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * Spec #803 — 권한 관리 admin 의 도메인 예외.
 */

class ProfileNotFoundException(profileId: Long) : BusinessException(
    errorCode = "PROFILE_NOT_FOUND",
    message = "Profile($profileId) 를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND,
)

class PermissionSetNotFoundException(permissionSetId: Long) : BusinessException(
    errorCode = "PERMISSION_SET_NOT_FOUND",
    message = "PermissionSet($permissionSetId) 를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND,
)
