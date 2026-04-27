package com.otoki.powersales.sap.auth.filter

import com.otoki.powersales.common.dto.ApiResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper

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
        val body = ApiResponse.error<Any>(code, message)
        response.writer.write(objectMapper.writeValueAsString(body))
        response.writer.flush()
    }
}
