package com.otoki.internal.sap.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.config.SapAuthProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

class SapApiKeyFilter(
    private val sapAuthProperties: SapAuthProperties,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private const val API_KEY_HEADER = "X-API-Key"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val apiKey = request.getHeader(API_KEY_HEADER)

        // API Key 검증
        if (apiKey.isNullOrBlank() || !timingSafeEquals(apiKey, sapAuthProperties.apiKey)) {
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "401", "INVALID_API_KEY")
            return
        }

        // IP 화이트리스트 검증
        val allowedIps = sapAuthProperties.getAllowedIpList()
        if (allowedIps.isNotEmpty()) {
            val clientIp = request.remoteAddr
            if (clientIp !in allowedIps) {
                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "403", "ACCESS_DENIED")
                return
            }
        }

        // SecurityContext에 SAP 인증 정보 설정
        val authentication = UsernamePasswordAuthenticationToken(
            "SAP_SYSTEM",
            null,
            listOf(SimpleGrantedAuthority("ROLE_SAP"))
        )
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }

    private fun timingSafeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    private fun writeErrorResponse(
        response: HttpServletResponse,
        httpStatus: Int,
        resultCode: String,
        resultMsg: String
    ) {
        response.status = httpStatus
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = mapOf("result_code" to resultCode, "result_msg" to resultMsg)
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
