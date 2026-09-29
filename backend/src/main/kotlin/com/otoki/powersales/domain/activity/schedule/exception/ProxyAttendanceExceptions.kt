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

/**
 * 미래 근무일에 대한 대리출근 등록 시도.
 *
 * 레거시(`mngDaily.jsp` `btn-add-sch`)는 "당일이 아니면 불가" 로 과거·미래를 함께 막았으나,
 * 신규는 과거일 소급 등록을 허용하고 미래일만 차단한다 (legacy-deviation.md §3 API 계약).
 */
class ProxyAttendanceFutureDateException : BusinessException(
    errorCode = "PROXY_ATTENDANCE_FUTURE_DATE",
    message = "미래 일정은 대리출근 등록할 수 없습니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)
