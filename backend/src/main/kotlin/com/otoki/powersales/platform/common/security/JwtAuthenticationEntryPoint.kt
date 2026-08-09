package com.otoki.powersales.platform.common.security

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.platform.common.dto.ApiResponse
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
        val isSessionInvalidated = request.getAttribute("jwt.sessionInvalidated") == true

        // 단말 회수(DEVICE_REVOKED) / 발급시각 컷오프(SESSION_INVALIDATED)는 만료보다 우선 —
        // 둘 다 갱신으로 회복되지 않으므로, 모바일이 refresh 시도 없이 강제 로그아웃으로 분기한다.
        val (errorCode, errorMessage) = when {
            isDeviceRevoked -> "DEVICE_REVOKED" to "다른 기기에서 로그인되어 로그아웃되었습니다"
            isSessionInvalidated -> "SESSION_INVALIDATED" to "시스템 데이터 정비로 다시 로그인이 필요합니다"
            isExpired -> "TOKEN_EXPIRED" to "토큰이 만료되었습니다"
            else -> "UNAUTHORIZED" to "인증이 필요합니다"
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json;charset=UTF-8"

        val body = ApiResponse.error<Any>(errorCode, errorMessage)
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
