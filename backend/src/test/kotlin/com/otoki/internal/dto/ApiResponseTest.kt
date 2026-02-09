package com.otoki.internal.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * ApiResponse 테스트
 */
class ApiResponseTest {

    @Test
    fun `성공 응답이 올바르게 생성된다`() {
        // given
        val testData = mapOf("key" to "value")
        val message = "성공"

        // when
        val response = ApiResponse.success(testData, message)

        // then
        assertTrue(response.success)
        assertEquals(testData, response.data)
        assertEquals(message, response.message)
        assertNull(response.error)
        assertNotNull(response.timestamp)
    }

    @Test
    fun `에러 응답이 올바르게 생성된다`() {
        // given
        val errorCode = "TEST_ERROR"
        val errorMessage = "테스트 에러"

        // when
        val response = ApiResponse.error<Any>(errorCode, errorMessage)

        // then
        assertFalse(response.success)
        assertNull(response.data)
        assertNotNull(response.error)
        assertEquals(errorCode, response.error?.code)
        assertEquals(errorMessage, response.error?.message)
    }

    @Test
    fun `ErrorDetail로 에러 응답이 생성된다`() {
        // given
        val errorDetail = ErrorDetail("CUSTOM_ERROR", "커스텀 에러")

        // when
        val response = ApiResponse.error<Any>(errorDetail)

        // then
        assertFalse(response.success)
        assertEquals(errorDetail, response.error)
    }
}
