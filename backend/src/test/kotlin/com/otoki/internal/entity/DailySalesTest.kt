package com.otoki.internal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("DailySales Entity 테스트")
class DailySalesTest {

    @Test
    @DisplayName("DailySales Entity 생성 성공")
    fun createDailySales() {
        // Given
        val eventId = "EVT001"
        val employeeId = "12345"
        val salesDate = LocalDate.of(2026, 2, 12)
        val status = DailySales.STATUS_REGISTERED

        // When
        val dailySales = DailySales(
            eventId = eventId,
            employeeId = employeeId,
            salesDate = salesDate,
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            mainProductAmount = 10000,
            photoUrl = "https://example.com/photo.jpg",
            status = status
        )

        // Then
        assertThat(dailySales.eventId).isEqualTo(eventId)
        assertThat(dailySales.employeeId).isEqualTo(employeeId)
        assertThat(dailySales.salesDate).isEqualTo(salesDate)
        assertThat(dailySales.status).isEqualTo(status)
        assertThat(dailySales.mainProductPrice).isEqualTo(1000)
        assertThat(dailySales.mainProductQuantity).isEqualTo(10)
        assertThat(dailySales.mainProductAmount).isEqualTo(10000)
    }

    @Test
    @DisplayName("임시저장 상태로 DailySales 생성 성공")
    fun createDraftDailySales() {
        // Given
        val eventId = "EVT001"
        val employeeId = "12345"
        val salesDate = LocalDate.of(2026, 2, 12)
        val status = DailySales.STATUS_DRAFT

        // When
        val dailySales = DailySales(
            eventId = eventId,
            employeeId = employeeId,
            salesDate = salesDate,
            status = status
        )

        // Then
        assertThat(dailySales.status).isEqualTo(DailySales.STATUS_DRAFT)
        assertThat(dailySales.mainProductPrice).isNull()
        assertThat(dailySales.photoUrl).isNull()
    }

    @Test
    @DisplayName("대표제품만 입력한 DailySales 생성 성공")
    fun createDailySalesWithMainProductOnly() {
        // Given & When
        val dailySales = DailySales(
            eventId = "EVT001",
            employeeId = "12345",
            salesDate = LocalDate.of(2026, 2, 12),
            mainProductPrice = 2000,
            mainProductQuantity = 5,
            mainProductAmount = 10000,
            status = DailySales.STATUS_REGISTERED
        )

        // Then
        assertThat(dailySales.mainProductPrice).isEqualTo(2000)
        assertThat(dailySales.mainProductQuantity).isEqualTo(5)
        assertThat(dailySales.mainProductAmount).isEqualTo(10000)
        assertThat(dailySales.subProductCode).isNull()
        assertThat(dailySales.subProductQuantity).isNull()
        assertThat(dailySales.subProductAmount).isNull()
    }

    @Test
    @DisplayName("기타제품만 입력한 DailySales 생성 성공")
    fun createDailySalesWithSubProductOnly() {
        // Given & When
        val dailySales = DailySales(
            eventId = "EVT001",
            employeeId = "12345",
            salesDate = LocalDate.of(2026, 2, 12),
            subProductCode = "SUB001",
            subProductQuantity = 3,
            subProductAmount = 15000,
            status = DailySales.STATUS_REGISTERED
        )

        // Then
        assertThat(dailySales.subProductCode).isEqualTo("SUB001")
        assertThat(dailySales.subProductQuantity).isEqualTo(3)
        assertThat(dailySales.subProductAmount).isEqualTo(15000)
        assertThat(dailySales.mainProductPrice).isNull()
        assertThat(dailySales.mainProductQuantity).isNull()
        assertThat(dailySales.mainProductAmount).isNull()
    }
}
