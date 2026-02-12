package com.otoki.internal.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("DailySalesExceptions 테스트")
class DailySalesExceptionsTest {

    @Test
    @DisplayName("DailySalesInvalidParameterException 생성 성공")
    fun createDailySalesInvalidParameterException() {
        // When
        val exception = DailySalesInvalidParameterException()

        // Then
        assertThat(exception.errorCode).isEqualTo("INVALID_PARAMETER")
        assertThat(exception.message).isEqualTo("필수 입력 항목이 누락되었습니다")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("DailySalesInvalidParameterException 커스텀 메시지로 생성 성공")
    fun createDailySalesInvalidParameterExceptionWithCustomMessage() {
        // Given
        val customMessage = "대표제품 또는 기타제품 중 최소 하나를 입력해야 합니다"

        // When
        val exception = DailySalesInvalidParameterException(customMessage)

        // Then
        assertThat(exception.message).isEqualTo(customMessage)
    }

    @Test
    @DisplayName("DailySalesInvalidPhotoException 생성 성공")
    fun createDailySalesInvalidPhotoException() {
        // When
        val exception = DailySalesInvalidPhotoException()

        // Then
        assertThat(exception.errorCode).isEqualTo("INVALID_PHOTO")
        assertThat(exception.message).isEqualTo("사진 파일이 필요합니다")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("DailySalesInvalidProductException 생성 성공")
    fun createDailySalesInvalidProductException() {
        // When
        val exception = DailySalesInvalidProductException()

        // Then
        assertThat(exception.errorCode).isEqualTo("INVALID_PRODUCT")
        assertThat(exception.message).isEqualTo("유효하지 않은 제품입니다")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    @DisplayName("DailySalesAlreadyRegisteredException 생성 성공")
    fun createDailySalesAlreadyRegisteredException() {
        // When
        val exception = DailySalesAlreadyRegisteredException()

        // Then
        assertThat(exception.errorCode).isEqualTo("ALREADY_REGISTERED")
        assertThat(exception.message).isEqualTo("오늘 매출이 이미 등록되었습니다")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @DisplayName("EventPeriodExpiredException 생성 성공")
    fun createEventPeriodExpiredException() {
        // When
        val exception = EventPeriodExpiredException()

        // Then
        assertThat(exception.errorCode).isEqualTo("EVENT_PERIOD_EXPIRED")
        assertThat(exception.message).isEqualTo("행사 기간이 아닙니다")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
    }

    @Test
    @DisplayName("DailySalesForbiddenException 생성 성공")
    fun createDailySalesForbiddenException() {
        // When
        val exception = DailySalesForbiddenException()

        // Then
        assertThat(exception.errorCode).isEqualTo("FORBIDDEN")
        assertThat(exception.message).isEqualTo("등록 권한이 없습니다")
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
    }
}
