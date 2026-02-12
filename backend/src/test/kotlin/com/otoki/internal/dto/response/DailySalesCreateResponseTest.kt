package com.otoki.internal.dto.response

import com.otoki.internal.entity.DailySales
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("DailySalesCreateResponse DTO 테스트")
class DailySalesCreateResponseTest {

    @Test
    @DisplayName("REGISTERED 상태의 DailySales Entity로부터 응답 DTO 생성 성공")
    fun fromRegisteredDailySales() {
        // Given
        val dailySales = DailySales(
            id = 123L,
            eventId = "EVT001",
            employeeId = "12345",
            salesDate = LocalDate.of(2026, 2, 12),
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            mainProductAmount = 10000,
            subProductCode = "SUB001",
            subProductQuantity = 5,
            subProductAmount = 25000,
            photoUrl = "https://example.com/photo.jpg",
            status = DailySales.STATUS_REGISTERED,
            createdAt = LocalDateTime.of(2026, 2, 12, 10, 30, 0)
        )

        // When
        val response = DailySalesCreateResponse.from(dailySales)

        // Then
        assertThat(response.dailySalesId).isEqualTo("123")
        assertThat(response.salesDate).isEqualTo("2026-02-12")
        assertThat(response.totalAmount).isEqualTo(35000) // 10000 + 25000
        assertThat(response.status).isEqualTo(DailySales.STATUS_REGISTERED)
        assertThat(response.registeredAt).isEqualTo("2026-02-12T10:30:00")
    }

    @Test
    @DisplayName("DRAFT 상태의 DailySales Entity로부터 응답 DTO 생성 성공")
    fun fromDraftDailySales() {
        // Given
        val dailySales = DailySales(
            id = 456L,
            eventId = "EVT001",
            employeeId = "12345",
            salesDate = LocalDate.of(2026, 2, 12),
            status = DailySales.STATUS_DRAFT,
            createdAt = LocalDateTime.of(2026, 2, 12, 10, 15, 0)
        )

        // When
        val response = DailySalesCreateResponse.from(dailySales)

        // Then
        assertThat(response.dailySalesId).isEqualTo("456")
        assertThat(response.salesDate).isEqualTo("2026-02-12")
        assertThat(response.totalAmount).isNull()
        assertThat(response.status).isEqualTo(DailySales.STATUS_DRAFT)
        assertThat(response.registeredAt).isEqualTo("2026-02-12T10:15:00")
    }

    @Test
    @DisplayName("대표제품만 있는 경우 총금액 계산 성공")
    fun fromDailySalesWithMainProductOnly() {
        // Given
        val dailySales = DailySales(
            id = 789L,
            eventId = "EVT001",
            employeeId = "12345",
            salesDate = LocalDate.of(2026, 2, 12),
            mainProductPrice = 2000,
            mainProductQuantity = 20,
            mainProductAmount = 40000,
            status = DailySales.STATUS_REGISTERED,
            createdAt = LocalDateTime.of(2026, 2, 12, 11, 0, 0)
        )

        // When
        val response = DailySalesCreateResponse.from(dailySales)

        // Then
        assertThat(response.totalAmount).isEqualTo(40000)
    }

    @Test
    @DisplayName("기타제품만 있는 경우 총금액 계산 성공")
    fun fromDailySalesWithSubProductOnly() {
        // Given
        val dailySales = DailySales(
            id = 999L,
            eventId = "EVT001",
            employeeId = "12345",
            salesDate = LocalDate.of(2026, 2, 12),
            subProductCode = "SUB002",
            subProductQuantity = 3,
            subProductAmount = 15000,
            status = DailySales.STATUS_REGISTERED,
            createdAt = LocalDateTime.of(2026, 2, 12, 11, 30, 0)
        )

        // When
        val response = DailySalesCreateResponse.from(dailySales)

        // Then
        assertThat(response.totalAmount).isEqualTo(15000)
    }
}
