package com.otoki.internal.repository

import com.otoki.internal.entity.DailySales
import com.otoki.internal.entity.Event
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@DisplayName("DailySalesRepository 테스트")
class DailySalesRepositoryTest {

    @Autowired
    private lateinit var dailySalesRepository: DailySalesRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val testEventId = "EVT001"
    private val testEmployeeId = "12345"
    private val testSalesDate = LocalDate.of(2026, 2, 12)

    @BeforeEach
    fun setUp() {
        dailySalesRepository.deleteAll()
        testEntityManager.clear()

        // 테스트용 행사 생성
        val event = Event(
            eventId = testEventId,
            eventType = "프로모션",
            eventName = "진라면 행사",
            startDate = LocalDate.of(2026, 2, 1),
            endDate = LocalDate.of(2026, 2, 28),
            customerId = "CUST001",
            assigneeId = testEmployeeId,
            targetAmount = 1000000
        )
        testEntityManager.persistAndFlush(event)
        testEntityManager.clear()
    }

    @Test
    @DisplayName("일매출 저장 성공")
    fun saveDailySales() {
        // Given
        val dailySales = DailySales(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            mainProductAmount = 10000,
            photoUrl = "https://example.com/photo.jpg",
            status = DailySales.STATUS_REGISTERED
        )

        // When
        val saved = dailySalesRepository.save(dailySales)
        testEntityManager.flush()
        testEntityManager.clear()

        // Then
        val found = dailySalesRepository.findById(saved.id)
        assertThat(found).isPresent
        assertThat(found.get().eventId).isEqualTo(testEventId)
        assertThat(found.get().employeeId).isEqualTo(testEmployeeId)
        assertThat(found.get().salesDate).isEqualTo(testSalesDate)
        assertThat(found.get().status).isEqualTo(DailySales.STATUS_REGISTERED)
    }

    @Test
    @DisplayName("행사/사원/날짜/상태로 일매출 존재 여부 확인 - 존재함")
    fun existsByEventIdAndEmployeeIdAndSalesDateAndStatus_Exists() {
        // Given
        val dailySales = DailySales(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            status = DailySales.STATUS_REGISTERED
        )
        dailySalesRepository.save(dailySales)
        testEntityManager.flush()
        testEntityManager.clear()

        // When
        val exists = dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            status = DailySales.STATUS_REGISTERED
        )

        // Then
        assertThat(exists).isTrue
    }

    @Test
    @DisplayName("행사/사원/날짜/상태로 일매출 존재 여부 확인 - 존재하지 않음")
    fun existsByEventIdAndEmployeeIdAndSalesDateAndStatus_NotExists() {
        // When
        val exists = dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            status = DailySales.STATUS_REGISTERED
        )

        // Then
        assertThat(exists).isFalse
    }

    @Test
    @DisplayName("DRAFT 상태의 일매출 조회 성공")
    fun findByEventIdAndEmployeeIdAndSalesDateAndStatus_DraftExists() {
        // Given
        val draftDailySales = DailySales(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            mainProductPrice = 500,
            status = DailySales.STATUS_DRAFT
        )
        dailySalesRepository.save(draftDailySales)
        testEntityManager.flush()
        testEntityManager.clear()

        // When
        val found = dailySalesRepository.findByEventIdAndEmployeeIdAndSalesDateAndStatus(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            status = DailySales.STATUS_DRAFT
        )

        // Then
        assertThat(found).isPresent
        assertThat(found.get().status).isEqualTo(DailySales.STATUS_DRAFT)
        assertThat(found.get().mainProductPrice).isEqualTo(500)
    }

    @Test
    @DisplayName("DRAFT 상태의 일매출 조회 - 존재하지 않음")
    fun findByEventIdAndEmployeeIdAndSalesDateAndStatus_DraftNotExists() {
        // When
        val found = dailySalesRepository.findByEventIdAndEmployeeIdAndSalesDateAndStatus(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            status = DailySales.STATUS_DRAFT
        )

        // Then
        assertThat(found).isEmpty
    }

    @Test
    @DisplayName("동일한 행사/사원/날짜에 DRAFT와 REGISTERED가 분리되어 저장됨")
    fun saveDraftAndRegisteredSeparately() {
        // Given
        val draft = DailySales(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            status = DailySales.STATUS_DRAFT
        )
        val registered = DailySales(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = testSalesDate,
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            mainProductAmount = 10000,
            status = DailySales.STATUS_REGISTERED
        )

        // When
        dailySalesRepository.save(draft)
        dailySalesRepository.save(registered)
        testEntityManager.flush()
        testEntityManager.clear()

        // Then
        val draftExists = dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            testEventId, testEmployeeId, testSalesDate, DailySales.STATUS_DRAFT
        )
        val registeredExists = dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            testEventId, testEmployeeId, testSalesDate, DailySales.STATUS_REGISTERED
        )

        assertThat(draftExists).isTrue
        assertThat(registeredExists).isTrue
    }

    @Test
    @DisplayName("다른 날짜의 일매출은 별도로 관리됨")
    fun saveDifferentDates() {
        // Given
        val date1 = LocalDate.of(2026, 2, 12)
        val date2 = LocalDate.of(2026, 2, 13)

        val sales1 = DailySales(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = date1,
            status = DailySales.STATUS_REGISTERED
        )
        val sales2 = DailySales(
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = date2,
            status = DailySales.STATUS_REGISTERED
        )

        // When
        dailySalesRepository.save(sales1)
        dailySalesRepository.save(sales2)
        testEntityManager.flush()
        testEntityManager.clear()

        // Then
        val exists1 = dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            testEventId, testEmployeeId, date1, DailySales.STATUS_REGISTERED
        )
        val exists2 = dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            testEventId, testEmployeeId, date2, DailySales.STATUS_REGISTERED
        )

        assertThat(exists1).isTrue
        assertThat(exists2).isTrue
    }
}
