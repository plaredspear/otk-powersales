package com.otoki.powersales.admin.security

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.service.AdminDataScopeService
import com.otoki.powersales.admin.service.AdminPermissionResolver
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.common.security.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * Admin 권한 매트릭스 필터 — Mobile (`UserPrincipal`) 기반 구버전.
 *
 * 본 필터는 더 이상 활성 동작하지 않는다. Web 인증이 `User` 기반 [WebUserPrincipal] 로 전환된
 * 이후 동일 책임(Employee/DataScope/Permission holder 세팅 + @RequiresPermission 검사)은
 * [WebAdminContextFilter] 가 수행한다.
 *
 * 파일 자체는 다수 controller 테스트가 `@MockitoBean` 으로 mock 하기 위해 import 하고 있어
 * 즉시 삭제 시 광범위한 테스트 손상이 발생하므로 보존. 컴파일만 되고 어디에도 빈 등록되지
 * 않는다 (@Component 부재 + SecurityFilterChain 미등록).
 *
 * 후속 정리 작업에서 테스트 측 mock 선언 제거 + 본 파일 삭제 예정.
 */
class AdminAuthorityFilter(
    private val employeeRepository: EmployeeRepository,
    private val objectMapper: ObjectMapper,
    private val adminDataScopeService: AdminDataScopeService,
    private val adminEmployeeHolder: AdminEmployeeHolder,
    private val dataScopeHolder: DataScopeHolder,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping,
    private val adminPermissionResolver: AdminPermissionResolver
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            filterChain.doFilter(request, response)
            return
        }

        val principal = authentication.principal
        if (principal !is UserPrincipal) {
            filterChain.doFilter(request, response)
            return
        }

        val employee = employeeRepository.findWithEmployeeInfoById(principal.userId)
        if (employee == null) {
            writeErrorResponse(response, "FORBIDDEN", "관리자 권한이 없습니다")
            return
        }
        val permissions = adminPermissionResolver.resolve(employee)

        if (permissions.isEmpty()) {
            writeErrorResponse(response, "FORBIDDEN", "관리자 권한이 없습니다")
            return
        }

        // Permission 체크: @RequiresPermission 어노테이션 확인
        val handlerMethod = resolveHandlerMethod(request)
        if (handlerMethod != null) {
            val annotation = handlerMethod.getMethodAnnotation(RequiresPermission::class.java)
            if (annotation != null) {
                val required = annotation.value.toSet()
                if (permissions.intersect(required).isEmpty()) {
                    writeErrorResponse(response, "PERMISSION_DENIED", "해당 API에 대한 접근 권한이 없습니다")
                    return
                }
            }
        }

        // Employee + DataScope를 holder에 저장 (서비스에서 재사용)
        adminEmployeeHolder.employee = employee
        val dataScope = adminDataScopeService.resolve(employee)
        dataScopeHolder.dataScope = dataScope

        filterChain.doFilter(request, response)
    }

    private fun resolveHandlerMethod(request: HttpServletRequest): HandlerMethod? {
        return try {
            val handler = requestMappingHandlerMapping.getHandler(request)?.handler
            handler as? HandlerMethod
        } catch (_: Exception) {
            null
        }
    }

    private fun writeErrorResponse(response: HttpServletResponse, code: String, message: String) {
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val errorResponse = ApiResponse.error<Any>(code, message)
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
