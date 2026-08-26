package com.otoki.powersales.admin.exception

import com.otoki.powersales.platform.common.exception.BusinessException
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

/**
 * 프로파일 수동 변경을 시스템 관리자가 아닌 사용자가 시도한 경우.
 *
 * 프로파일은 권한 모델의 최상위 입력이라, `user:EDIT` 만 가진 관리자에게 열어두면
 * 자신의 프로파일을 상위로 올려 권한을 자가 상승시킬 수 있다. 컨트롤러의
 * `@RequiresSfPermission(user, EDIT)` 위에 시스템 관리자 판정을 한 겹 더 둔다.
 */
class ProfileChangeForbiddenException : BusinessException(
    errorCode = "PROFILE_CHANGE_FORBIDDEN",
    message = "프로파일 변경은 시스템 관리자만 가능합니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 자기 자신의 프로파일을 변경하려고 시도한 경우 (권한 자가 상승/이탈 방지).
 *
 * 시스템 관리자라도 본인 프로파일을 낮추면 스스로 권한을 잃어 복구 불가 상태가 되고,
 * 반대로 올리는 것은 승인 없는 권한 확대다. 두 방향 모두 차단한다.
 */
class CannotChangeOwnProfileException : BusinessException(
    errorCode = "CANNOT_CHANGE_OWN_PROFILE",
    message = "자기 자신의 프로파일은 변경할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 요청한 profileId 가 profile 테이블에 없는 경우.
 */
class AdminProfileNotFoundException(profileId: Long) : BusinessException(
    errorCode = "PROFILE_NOT_FOUND",
    message = "Profile($profileId) 를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
