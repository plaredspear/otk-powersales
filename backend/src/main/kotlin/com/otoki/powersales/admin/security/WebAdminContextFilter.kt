package com.otoki.powersales.admin.security

import com.otoki.powersales.auth.permission.AdminPermissionCache
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionEvaluator
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
 * Web Admin 컨텍스트 필터 (spec #801 — SF 권한 모델 전면 적용).
 *
 * `/api/v1/admin/` 하위 경로에서 [WebUserPrincipal] 이 인증된 요청에 대해:
 *  1. [AdminDataScopeService] 로 DataScope 산출 → attribute ([AdminContextAttributes.DATA_SCOPE]) 세팅
 *  2. Principal 에 박혀있는 SF 권한 snapshot (Set<String>) 을 attribute
 *     ([AdminContextAttributes.PERMISSIONS]) 로 노출 + controller handler 의 `@RequiresSfPermission`
 *     어노테이션과 매칭 검사
 *
 * Permission 은 [AdminPermissionCache] 가 lazy lookup (5분 TTL). spec #808 expandAllDataBits 이후
 * permission set 이 365+ key 로 커지면서 JWT claim 운반 시 토큰 byte 가 8KB 초과 → nginx
 * `large_client_header_buffers` 한도 (4 8k) 초과로 `400 Request Header Or Cookie Too Large`
 * 발생한 사례를 해소. cache TTL 동안 permission 변경 반영이 지연될 수 있고, 즉시 반영이 필요하면
 * AdminPermissionCache.invalidate(userId) 명시 호출.
 *
 * Principal 의 `employeeId` snapshot 이 비어있는 사용자 (ADMIN-* 부트스트랩 등) 는 permission 빈 셋.
 * `@RequiresSfPermission` 부착 endpoint 접근 시 403, 미부착 endpoint 는 통과.
 */
class WebAdminContextFilter(
    private val adminDataScopeCache: AdminDataScopeCache,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val sfPermissionEvaluator: SfPermissionEvaluator,
    private val adminPermissionCache: AdminPermissionCache,
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
            val permissions: Set<String> = if (principal.employeeId != null) {
                request.setAttribute(AdminContextAttributes.DATA_SCOPE, adminDataScopeCache.get(principal))
                val resolved = adminPermissionCache.get(principal.userId)
                request.setAttribute(AdminContextAttributes.PERMISSIONS, resolved)
                resolved
            } else {
                emptySet()
            }

            val handlerMethod = resolveHandlerMethod(request)
            val required = handlerMethod
                ?.getMethodAnnotation(RequiresSfPermission::class.java)

            if (required != null) {
                if (!sfPermissionEvaluator.isAllowed(required, permissions)) {
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
