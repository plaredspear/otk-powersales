package com.otoki.powersales.admin.tools.logging.controller

import com.otoki.powersales.admin.tools.logging.dto.LoggerLevelResponse
import com.otoki.powersales.admin.tools.logging.dto.LoggerListResponse
import com.otoki.powersales.admin.tools.logging.dto.UpdateLoggerLevelRequest
import com.otoki.powersales.admin.tools.logging.service.LogLevelService
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자 도구 > 대시보드 — 런타임 로그 레벨 조회/변경 컨트롤러.
 *
 * 운영 부하/PII 노출에 직접 영향을 주는 강력한 기능이라 **시스템 관리자 전용**으로 가드한다.
 * `@RequiresSfPermission` 은 entity 단위 CRUD 권한이라 이 성격에 맞지 않아, 컨트롤러 내부에서
 * [SystemAdminProfilePolicy.isSystemAdmin] 으로 직접 판정한다 (WebAdminContextFilter 의 시스템
 * 관리자 우회 식별자와 동일 기준).
 *
 * actuator `/actuator/loggers` 는 Spring Security 체인 밖(인증 없이 접근 가능)이므로 열지 않고,
 * `/api/v1/admin/` 하위에서 web admin 인증(WebJwtAuthenticationFilter) + 시스템 관리자 가드를
 * 적용한다.
 */
@RestController
@RequestMapping("/api/v1/admin/tools/log-levels")
class LogLevelController(
    private val logLevelService: LogLevelService,
) {

    @GetMapping
    fun getLoggers(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<LoggerListResponse>> {
        requireSystemAdmin(principal)
        return ResponseEntity.ok(ApiResponse.success(logLevelService.getLoggers()))
    }

    @PostMapping
    fun updateLoggerLevel(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: UpdateLoggerLevelRequest,
    ): ResponseEntity<ApiResponse<LoggerLevelResponse>> {
        requireSystemAdmin(principal)
        val updated = logLevelService.setLevel(request.loggerName, request.level)
        return ResponseEntity.ok(ApiResponse.success(updated))
    }

    private fun requireSystemAdmin(principal: WebUserPrincipal) {
        if (!SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)) {
            throw BusinessException(
                errorCode = "PERMISSION_DENIED",
                message = "로그 레벨 관리는 시스템 관리자만 사용할 수 있습니다",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }
}
