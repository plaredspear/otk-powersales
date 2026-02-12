package com.otoki.internal.dto.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile

@DisplayName("DailySalesCreateRequest DTO 테스트")
class DailySalesCreateRequestTest {

    @Test
    @DisplayName("대표제품 정보가 완전히 입력된 경우 hasMainProduct() true 반환")
    fun hasMainProduct_Complete() {
        // Given
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10
        )

        // When & Then
        assertThat(request.hasMainProduct()).isTrue
    }

    @Test
    @DisplayName("대표제품 정보가 부분적으로 입력된 경우 hasMainProduct() false 반환")
    fun hasMainProduct_Partial() {
        // Given
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000
        )

        // When & Then
        assertThat(request.hasMainProduct()).isFalse
    }

    @Test
    @DisplayName("기타제품 정보가 완전히 입력된 경우 hasSubProduct() true 반환")
    fun hasSubProduct_Complete() {
        // Given
        val request = DailySalesCreateRequest(
            subProductCode = "SUB001",
            subProductQuantity = 5,
            subProductAmount = 25000
        )

        // When & Then
        assertThat(request.hasSubProduct()).isTrue
    }

    @Test
    @DisplayName("기타제품 정보가 부분적으로 입력된 경우 hasSubProduct() false 반환")
    fun hasSubProduct_Partial() {
        // Given
        val request = DailySalesCreateRequest(
            subProductCode = "SUB001",
            subProductQuantity = 5
        )

        // When & Then
        assertThat(request.hasSubProduct()).isFalse
        assertThat(request.hasPartialSubProduct()).isTrue
    }

    @Test
    @DisplayName("기타제품 정보가 모두 누락된 경우 hasPartialSubProduct() false 반환")
    fun hasPartialSubProduct_Empty() {
        // Given
        val request = DailySalesCreateRequest()

        // When & Then
        assertThat(request.hasPartialSubProduct()).isFalse
    }

    @Test
    @DisplayName("대표제품 총금액 계산 성공")
    fun calculateMainProductAmount() {
        // Given
        val request = DailySalesCreateRequest(
            mainProductPrice = 2000,
            mainProductQuantity = 15
        )

        // When
        val amount = request.calculateMainProductAmount()

        // Then
        assertThat(amount).isEqualTo(30000)
    }

    @Test
    @DisplayName("대표제품 정보 미입력 시 총금액 null 반환")
    fun calculateMainProductAmount_NoMainProduct() {
        // Given
        val request = DailySalesCreateRequest()

        // When
        val amount = request.calculateMainProductAmount()

        // Then
        assertThat(amount).isNull()
    }

    @Test
    @DisplayName("사진 파일이 있는 경우")
    fun withPhoto() {
        // Given
        val photo = MockMultipartFile(
            "photo",
            "test.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            photo = photo
        )

        // When & Then
        assertThat(request.photo).isNotNull
        assertThat(request.photo?.originalFilename).isEqualTo("test.jpg")
    }
}
