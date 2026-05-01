package com.otoki.powersales.common.exception

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.sap.inbound.dto.SapResultWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.context.request.ServletWebRequest

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    @DisplayName("handleMethodNotSupported - SAP 인바운드 path 는 SapResultWrapper 형식")
    fun handleMethodNotSupported_sapPathReturnsSapResultWrapper() {
        val ex = HttpRequestMethodNotSupportedException("GET", listOf("POST"))
        val request = ServletWebRequest(MockHttpServletRequest("GET", "/api/v1/sap/employee"))

        val response = handler.handleMethodNotSupported(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
        val body = response.body as SapResultWrapper<*>
        assertThat(body.resultCode).isEqualTo(SapResultWrapper.CODE_METHOD_NOT_ALLOWED)
        assertThat(body.resultMsg).contains("POST")
    }

    @Test
    @DisplayName("handleMethodNotSupported - 비-SAP path 는 ApiResponse 형식 (기존 컨벤션 유지)")
    fun handleMethodNotSupported_nonSapPathReturnsApiResponse() {
        val ex = HttpRequestMethodNotSupportedException("GET", listOf("POST"))
        val request = ServletWebRequest(MockHttpServletRequest("GET", "/api/v1/admin/auth/login"))

        val response = handler.handleMethodNotSupported(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        assertThat(response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
        val body = response.body as ApiResponse<*>
        assertThat(body.success).isFalse()
        assertThat(body.error?.code).isEqualTo("METHOD_NOT_ALLOWED")
    }

    @Test
    @DisplayName("handleMethodNotSupported - 모바일 path 도 ApiResponse 형식")
    fun handleMethodNotSupported_mobilePathReturnsApiResponse() {
        val ex = HttpRequestMethodNotSupportedException("DELETE", listOf("POST"))
        val request = ServletWebRequest(MockHttpServletRequest("DELETE", "/api/v1/mobile/auth/login"))

        val response = handler.handleMethodNotSupported(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
        val body = response.body as ApiResponse<*>
        assertThat(body.error?.code).isEqualTo("METHOD_NOT_ALLOWED")
    }
}
