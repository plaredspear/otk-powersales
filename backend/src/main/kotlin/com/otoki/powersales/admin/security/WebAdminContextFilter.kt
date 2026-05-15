package com.otoki.powersales.admin.security

import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.admin.scope.AdminPermissionHolder
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.admin.service.AdminPermissionResolver
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.employee.repository.EmployeeRepository
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
 *  1. Employee 조회 → [AdminEmployeeHolder] 세팅 (request scope)
 *  2. [AdminDataScopeService] 로 DataScope 산출 → [DataScopeHolder] 세팅
 *  3. [AdminPermissionResolver] 로 effective permission 산출 → [AdminPermissionHolder] 세팅
 *  4. controller handler 의 `@RequiresPermission` 어노테이션과 effective permission 교집합 검사
 *
 * Employee 미존재 (ADMIN-* 부트스트랩 / SAP 미동기) 사용자는 holder 미세팅 + permission 빈 셋.
 * `@RequiresPermission` 부착 endpoint 접근 시 403, 미부착 endpoint (예: `/auth/me`, `/auth/logout`) 는 통과.
 *
 * [com.otoki.powersales.auth.web.WebJwtAuthenticationFilter] 직후 chain 에 등록 — JWT 로 복원된
 * `SecurityContextHolder.authentication.principal` 이 본 필터 진입 조건.
 */
class WebAdminContextFilter(
    private val employeeRepository: EmployeeRepository,
    private val adminDataScopeService: AdminDataScopeService,
    private val adminPermissionResolver: AdminPermissionResolver,
    private val adminEmployeeHolder: AdminEmployeeHolder,
    private val dataScopeHolder: DataScopeHolder,
    private val adminPermissionHolder: AdminPermissionHolder,
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
            val employee = principal.employeeId?.let { employeeRepository.findWithEmployeeInfoById(it) }

            if (employee != null) {
                adminEmployeeHolder.employee = employee
                dataScopeHolder.dataScope = adminDataScopeService.resolve(employee)
                adminPermissionHolder.permissions = adminPermissionResolver.resolve(employee)
            }

            val handlerMethod = resolveHandlerMethod(request)
            val required = handlerMethod
                ?.getMethodAnnotation(RequiresPermission::class.java)
                ?.value
                ?.toSet()

            if (required != null && required.isNotEmpty()) {
                if (adminPermissionHolder.permissions.intersect(required).isEmpty()) {
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
