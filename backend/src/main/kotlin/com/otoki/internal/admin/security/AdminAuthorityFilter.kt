package com.otoki.internal.admin.security

import com.fasterxml.jackson.databind.ObjectMapper
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

@Component
class AdminAuthorityFilter(
    private val userRepository: UserRepository,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        val ALLOWED_AUTHORITIES = setOf("조장", "지점장", "영업부장", "사업부장", "영업본부장", "영업지원실")
    }

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

        if (appAuthority == null || appAuthority !in ALLOWED_AUTHORITIES) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            val errorResponse = ApiResponse.error<Any>("FORBIDDEN", "관리자 권한이 없습니다")
            response.writer.write(objectMapper.writeValueAsString(errorResponse))
            return
        }

        filterChain.doFilter(request, response)
    }
}
