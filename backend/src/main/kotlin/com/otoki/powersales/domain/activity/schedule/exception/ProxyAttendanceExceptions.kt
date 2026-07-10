package com.otoki.powersales.domain.activity.schedule.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

// ===== AccountViewAll 대리출근 (지점 선택형) =====

/** AccountViewAll 권한이 아닌 사용자가 대리출근 API 에 접근. */
class ProxyAttendanceNotAllowedException : BusinessException(
    errorCode = "PROXY_ATTENDANCE_NOT_ALLOWED",
    message = "대리출근 권한이 없습니다.",
    httpStatus = HttpStatus.FORBIDDEN
)

/** 요청 지점이 대리출근 허용 지점 목록에 없음 (IDOR 방어). */
class ProxyAttendanceBranchNotAllowedException : BusinessException(
    errorCode = "PROXY_ATTENDANCE_BRANCH_NOT_ALLOWED",
    message = "선택할 수 없는 지점입니다.",
    httpStatus = HttpStatus.FORBIDDEN
)

/** 대상 여사원이 선택한 지점 소속이 아님. */
class ProxyAttendanceNotBranchMemberException : BusinessException(
    errorCode = "PROXY_ATTENDANCE_NOT_BRANCH_MEMBER",
    message = "선택한 지점 소속 여사원만 대리출근을 등록할 수 있습니다.",
    httpStatus = HttpStatus.FORBIDDEN
)
