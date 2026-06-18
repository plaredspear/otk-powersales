package com.otoki.powersales.domain.activity.inspection.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 지점 스코프 밖 테마 접근 시 — 403. themeId 추측으로 타 지점 테마의 상세/엑셀을 들여다보는 것을 차단한다.
 */
class InspectionThemeForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)
