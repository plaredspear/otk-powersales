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

class AssignmentNotFoundException(assignmentId: Long) : BusinessException(
    errorCode = "ASSIGNMENT_NOT_FOUND",
    message = "Assignment($assignmentId) 를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND,
)

class AssignmentAlreadyExistsException(userId: Long, permissionSetFlagsId: Long) : BusinessException(
    errorCode = "ASSIGNMENT_ALREADY_EXISTS",
    message = "User($userId) 에게 PermissionSet($permissionSetFlagsId) 가 이미 부여되어 있습니다",
    httpStatus = HttpStatus.CONFLICT,
)

class CannotRevokeSelfException : BusinessException(
    errorCode = "CANNOT_REVOKE_SELF",
    message = "자기 자신의 MANAGE_USERS 권한은 회수할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST,
)

class LastAdminGuardException : BusinessException(
    errorCode = "LAST_ADMIN_GUARD",
    message = "회수 후 MANAGE_USERS 권한 보유 사용자가 없습니다. 최소 1명의 admin 이 필요합니다",
    httpStatus = HttpStatus.BAD_REQUEST,
)

class PermissionSetFlagsNotFoundException(permissionSetFlagsId: Long) : BusinessException(
    errorCode = "PERMISSION_SET_FLAGS_NOT_FOUND",
    message = "PermissionSetFlags($permissionSetFlagsId) 를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND,
)

class AssignmentUserNotFoundException(userId: Long) : BusinessException(
    errorCode = "ASSIGNMENT_USER_NOT_FOUND",
    message = "User($userId) 를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND,
)
