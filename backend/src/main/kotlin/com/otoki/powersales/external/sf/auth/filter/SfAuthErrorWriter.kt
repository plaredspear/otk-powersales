package com.otoki.powersales.external.sf.auth.filter

import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper

/**
 * SF 인바운드 경로의 인증/인가 필터에서 발생하는 거부 응답 직렬화 헬퍼.
 *
 * RFC 6749 §5.2 표준 OAuth 에러 응답 형식 `{"error":..., "error_description":...}` 으로 직렬화한다.
 * SAP 측 [com.otoki.powersales.external.sap.auth.filter.SapAuthErrorWriter] 는 SapResultWrapper 형식을
 * 사용하지만 SF 는 SF Apex Named Credential / 표준 OAuth client 가 응답을 표준 형식으로 기대하기
 * 때문에 RFC 6749 형식을 직접 사용한다.
 */
object SfAuthErrorWriter {

    fun write(
        response: HttpServletResponse,
        objectMapper: ObjectMapper,
        status: Int,
        error: String,
        errorDescription: String
    ) {
        if (response.isCommitted) return
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val body = mapOf(
            "error" to error.lowercase(),
            "error_description" to errorDescription
        )
        response.writer.write(objectMapper.writeValueAsString(body))
        response.writer.flush()
    }
}
