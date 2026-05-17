package com.otoki.powersales.admin.security

import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.ObjectMapper

/**
 * Web Admin 컨텍스트 필터.
 *
 * `/api/v1/admin/` 하위 경로에서 [WebUserPrincipal] 이 인증된 요청에 대해:
 *  1. [AdminDataScopeService] 로 DataScope 산출 → attribute ([AdminContextAttributes.DATA_SCOPE]) 세팅
 *  2. Principal 에 박혀있는 effective permission snapshot 을 attribute ([AdminContextAttributes.PERMISSIONS])
 *     로 노출 + controller handler 의 `@RequiresPermission` 어노테이션과 교집합 검사
 *
 * Permission 은 JWT claim 으로 운반되어 로그인 시점 1회 산출 — 매 요청 DB 조회 없음. 권한 변경 후
 * 즉시 반영이 필요한 경우 사용자 재로그인 안내 (token TTL = max stale window).
 *
 * Principal 의 `employeeId` snapshot 이 비어있는 사용자 (ADMIN-* 부트스트랩 등) 는 permission 빈 셋.
 * `@RequiresPermission` 부착 endpoint 접근 시 403, 미부착 endpoint (예: `/auth/me`, `/auth/logout`) 는 통과.
 *
 * Controller 는 holder 빈 대신 `@AuthenticationPrincipal WebUserPrincipal` 로 인증 컨텍스트를 직접 수신.
 *
 * [com.otoki.powersales.auth.web.WebJwtAuthenticationFilter] 직후 chain 에 등록 — JWT 로 복원된
 * `SecurityContextHolder.authentication.principal` 이 본 필터 진입 조건.
 */
class WebAdminContextFilter(
    private val adminDataScopeService: AdminDataScopeService,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal as? WebUserPrincipal

        if (principal != null) {
            val permissions = if (principal.employeeId != null) {
                request.setAttribute(AdminContextAttributes.DATA_SCOPE, adminDataScopeService.resolve(principal))
                request.setAttribute(AdminContextAttributes.PERMISSIONS, principal.permissions)
                principal.permissions
            } else {
                emptySet()
            }

            val handlerMethod = resolveHandlerMethod(request)
            val required = handlerMethod
                ?.getMethodAnnotation(RequiresPermission::class.java)
                ?.value
                ?.toSet()

            if (required != null && required.isNotEmpty()) {
                if (permissions.intersect(required).isEmpty()) {
                    writeErrorResponse(response, "PERMISSION_DENIED", "해당 API에 대한 접근 권한이 없습니다")
                    return
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveHandlerMethod(request: HttpServletRequest): HandlerMethod? {
        return try {
            requestMappingHandlerMapping.getHandler(request)?.handler as? HandlerMethod
        } catch (_: Exception) {
            null
        }
    }

    private fun writeErrorResponse(response: HttpServletResponse, code: String, message: String) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = ApiResponse.error<Any>(code, message)
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
