package com.otoki.powersales.common.security

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.common.dto.ApiResponse
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
        val isDeviceRevoked = request.getAttribute("jwt.deviceRevoked") == true

        // 단말 회수/교체(DEVICE_REVOKED)는 만료보다 우선 — 모바일이 강제 로그아웃으로 분기.
        val (errorCode, errorMessage) = when {
            isDeviceRevoked -> "DEVICE_REVOKED" to "다른 기기에서 로그인되어 로그아웃되었습니다"
            isExpired -> "TOKEN_EXPIRED" to "토큰이 만료되었습니다"
            else -> "UNAUTHORIZED" to "인증이 필요합니다"
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"

        val body = ApiResponse.error<Any>(errorCode, errorMessage)
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
