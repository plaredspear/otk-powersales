package com.otoki.powersales.platform.common.exception

import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.external.sap.inbound.dto.SapResultWrapper
import com.otoki.powersales.platform.common.exception.GlobalExceptionHandler
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

    @Test
    @DisplayName("handleBusinessException - 5xx(서버 결함)도 상태/에러코드를 그대로 응답한다 (로깅은 error 레벨)")
    fun handleBusinessException_serverError() {
        val ex = BusinessException(
            errorCode = "STORAGE_WRITE_FAILED",
            message = "스토리지 저장에 실패했습니다",
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            cause = RuntimeException("s3 put failed")
        )
        val request = ServletWebRequest(MockHttpServletRequest("POST", "/api/v1/admin/notices/images/inline"))

        val response = handler.handleBusinessException(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = response.body as ApiResponse<*>
        assertThat(body.success).isFalse()
        assertThat(body.error?.code).isEqualTo("STORAGE_WRITE_FAILED")
        assertThat(body.error?.message).isEqualTo("스토리지 저장에 실패했습니다")
    }

    @Test
    @DisplayName("handleBusinessException - 4xx(클라이언트 오류)는 상태/에러코드를 그대로 응답한다 (로깅은 warn 레벨)")
    fun handleBusinessException_clientError() {
        val ex = BusinessException(
            errorCode = "INVALID_PARAMETER",
            message = "잘못된 요청",
            httpStatus = HttpStatus.BAD_REQUEST
        )
        val request = ServletWebRequest(MockHttpServletRequest("POST", "/api/v1/admin/notices"))

        val response = handler.handleBusinessException(ex, request)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        val body = response.body as ApiResponse<*>
        assertThat(body.error?.code).isEqualTo("INVALID_PARAMETER")
    }
}
