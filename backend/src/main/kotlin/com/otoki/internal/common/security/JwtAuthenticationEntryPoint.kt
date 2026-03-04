package com.otoki.internal.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.common.dto.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val isExpired = request.getAttribute("jwt.expired") == true

        val errorCode = if (isExpired) "TOKEN_EXPIRED" else "UNAUTHORIZED"
        val errorMessage = if (isExpired) "토큰이 만료되었습니다" else "인증이 필요합니다"

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"

        val body = ApiResponse.error<Any>(errorCode, errorMessage)
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
