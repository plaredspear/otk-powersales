package com.otoki.powersales.sap.auth.filter

import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper

/**
 * SAP 인바운드 경로의 인증/인가/IP 필터에서 발생하는 거부 응답 직렬화 헬퍼.
 *
 * Filter 단계에서 발생하는 401/403 은 [com.otoki.powersales.sap.inbound.controller.SapInboundExceptionHandler]
 * 의 영향권 밖이므로, 응답 형식이 다른 SAP 인바운드 응답(SapResultWrapper) 과 일관되도록
 * 여기서 직접 [SapResultWrapper] 로 직렬화한다.
 */
object SapAuthErrorWriter {

    fun write(
        response: HttpServletResponse,
        objectMapper: ObjectMapper,
        status: Int,
        code: String,
        message: String
    ) {
        if (response.isCommitted) return
        response.status = status
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        val body = SapResultWrapper<Any>(resultCode = code, resultMsg = message, resultDetail = null)
        response.writer.write(objectMapper.writeValueAsString(body))
        response.writer.flush()
    }
}
