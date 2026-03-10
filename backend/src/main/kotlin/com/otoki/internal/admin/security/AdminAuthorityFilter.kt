package com.otoki.internal.admin.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.admin.service.AdminDataScopeService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.common.security.UserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Component
class AdminAuthorityFilter(
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper,
    private val adminDataScopeService: AdminDataScopeService,
    private val dataScopeHolder: DataScopeHolder,
    private val requestMappingHandlerMapping: RequestMappingHandlerMapping
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

        val user = userRepository.findById(principal.userId).orElse(null)
        val appAuthority = user?.appAuthority

        val permissions = AdminRolePermissions.getPermissions(appAuthority)

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

        // DataScope resolve + holder에 저장
        val dataScope = adminDataScopeService.resolve(principal.userId)
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
