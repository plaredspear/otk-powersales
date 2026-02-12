package com.otoki.internal.service

import com.otoki.internal.dto.request.EventListRequest
import com.otoki.internal.entity.*
import com.otoki.internal.exception.EventNotFoundException
import com.otoki.internal.repository.EventProductRepository
import com.otoki.internal.repository.EventRepository
import com.otoki.internal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("EventService 테스트")
class EventServiceTest {

    @Mock
    private lateinit var eventRepository: EventRepository

    @Mock
    private lateinit var eventProductRepository: EventProductRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var eventService: EventService

    // ========== getEvents Tests ==========

    @Nested
    @DisplayName("getEvents - 행사 목록 조회")
    inner class GetEventsTests {

        @Test
        @DisplayName("담당자의 행사 목록을 정상 조회한다")
        fun getEvents_success() {
            // Given
            val userId = 1L
            val employeeId = "EMP001"
            val user = createUser(id = userId, employeeId = employeeId)
            val request = EventListRequest(
                customerId = null,
                date = "2026-02-12",
                page = 0,
                size = 10
            )

            val events = listOf(
                createEvent(
                    id = 1,
                    eventId = "EVT001",
                    assigneeId = employeeId,
                    customerId = "C001",
                    startDate = LocalDate.of(2026, 2, 10),
                    endDate = LocalDate.of(2026, 2, 28)
                )
            )
            val page = PageImpl(events, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate")), 1)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(eventRepository.findEventsByAssignee(
                eq(employeeId),
                eq(null),
                eq(LocalDate.of(2026, 2, 12)),
                any()
            )).thenReturn(page)

            // When
            val result = eventService.getEvents(userId, request)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].eventId).isEqualTo("EVT001")
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(10)
            assertThat(result.totalElements).isEqualTo(1)
        }

        @Test
        @DisplayName("거래처 필터가 적용된다")
        fun getEvents_withCustomerFilter() {
            // Given
            val userId = 1L
            val employeeId = "EMP001"
            val user = createUser(id = userId, employeeId = employeeId)
            val request = EventListRequest(
                customerId = "C001",
                date = null,
                page = 0,
                size = 10
            )

            val page = PageImpl<Event>(emptyList(), PageRequest.of(0, 10), 0)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(eventRepository.findEventsByAssignee(
                eq(employeeId),
                eq("C001"),
                any(),
                any()
            )).thenReturn(page)

            // When
            val result = eventService.getEvents(userId, request)

            // Then
            assertThat(result.content).isEmpty()
        }
    }

    // ========== getEventDetail Tests ==========

    @Nested
    @DisplayName("getEventDetail - 행사 상세 조회")
    inner class GetEventDetailTests {

        @Test
        @DisplayName("행사 상세 정보를 정상 조회한다")
        fun getEventDetail_success() {
            // Given
            val userId = 1L
            val employeeId = "EMP001"
            val eventId = "EVT001"
            val user = createUser(id = userId, employeeId = employeeId)
            val event = createEvent(
                id = 1,
                eventId = eventId,
                assigneeId = employeeId,
                customerId = "C001",
                targetAmount = 5000000L,
                startDate = LocalDate.now().minusDays(5),
                endDate = LocalDate.now().plusDays(15)
            )
            val products = listOf(
                createEventProduct(id = 1, eventId = eventId, productCode = "P001", isMainProduct = true),
                createEventProduct(id = 2, eventId = eventId, productCode = "P002", isMainProduct = false)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event))
            whenever(eventProductRepository.findByEventId(eventId)).thenReturn(products)

            // When
            val result = eventService.getEventDetail(userId, eventId)

            // Then
            assertThat(result.event.eventId).isEqualTo(eventId)
            assertThat(result.event.assigneeId).isEqualTo(employeeId)
            assertThat(result.salesInfo.targetAmount).isEqualTo(5000000L)
            assertThat(result.products.mainProduct).isNotNull
            assertThat(result.products.mainProduct!!.productCode).isEqualTo("P001")
            assertThat(result.products.subProducts).hasSize(1)
            assertThat(result.canRegisterToday).isTrue()
        }

        @Test
        @DisplayName("진행율이 정확히 계산된다")
        fun getEventDetail_progressRateCalculation() {
            // Given
            val userId = 1L
            val employeeId = "EMP001"
            val eventId = "EVT001"
            val user = createUser(id = userId, employeeId = employeeId)
            val event = createEvent(
                id = 1,
                eventId = eventId,
                assigneeId = employeeId,
                startDate = LocalDate.now().minusDays(10),
                endDate = LocalDate.now().plusDays(10)
            )

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event))
            whenever(eventProductRepository.findByEventId(eventId)).thenReturn(emptyList())

            // When
            val result = eventService.getEventDetail(userId, eventId)

            // Then: 10일 경과 / 20일 전체 = 50%
            assertThat(result.salesInfo.progressRate).isEqualTo(50.0)
        }

        @Test
        @DisplayName("존재하지 않는 행사 조회 시 EventNotFoundException 발생")
        fun getEventDetail_notFound() {
            // Given
            val userId = 1L
            val employeeId = "EMP001"
            val eventId = "EVT999"
            val user = createUser(id = userId, employeeId = employeeId)

            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                eventService.getEventDetail(userId, eventId)
            }.isInstanceOf(EventNotFoundException::class.java)
        }
    }

    // ========== getDailySales Tests ==========

    @Nested
    @DisplayName("getDailySales - 일별 매출 목록 조회")
    inner class GetDailySalesTests {

        @Test
        @DisplayName("일별 매출 목록 조회 시 빈 리스트 반환 (F51 구현 전)")
        fun getDailySales_returnsEmptyList() {
            // Given
            val userId = 1L
            val eventId = "EVT001"
            val event = createEvent(id = 1, eventId = eventId)

            whenever(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event))

            // When
            val result = eventService.getDailySales(userId, eventId)

            // Then
            assertThat(result.dailySales).isEmpty()
        }

        @Test
        @DisplayName("존재하지 않는 행사 조회 시 EventNotFoundException 발생")
        fun getDailySales_eventNotFound() {
            // Given
            val userId = 1L
            val eventId = "EVT999"

            whenever(eventRepository.findByEventId(eventId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                eventService.getDailySales(userId, eventId)
            }.isInstanceOf(EventNotFoundException::class.java)
        }
    }

    // ========== Helper Functions ==========

    private fun createUser(
        id: Long,
        employeeId: String = "EMP001",
        name: String = "홍길동",
        department: String = "영업팀",
        branchName: String = "서울지점",
        role: UserRole = UserRole.USER,
        workerType: WorkerType = WorkerType.PATROL
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encoded_password",
            name = name,
            department = department,
            branchName = branchName,
            role = role,
            workerType = workerType
        )
    }

    private fun createEvent(
        id: Long,
        eventId: String = "EVT001",
        eventType: String = "[시식]",
        eventName: String = "상온(오뚜기카레_매운맛100G)",
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate = LocalDate.now().plusDays(10),
        customerId: String = "C001",
        assigneeId: String = "EMP001",
        targetAmount: Long = 0L
    ): Event {
        return Event(
            id = id,
            eventId = eventId,
            eventType = eventType,
            eventName = eventName,
            startDate = startDate,
            endDate = endDate,
            customerId = customerId,
            assigneeId = assigneeId,
            targetAmount = targetAmount
        )
    }

    private fun createEventProduct(
        id: Long,
        eventId: String = "EVT001",
        productCode: String = "P001",
        productName: String = "오뚜기카레",
        isMainProduct: Boolean = false
    ): EventProduct {
        return EventProduct(
            id = id,
            eventId = eventId,
            productCode = productCode,
            productName = productName,
            isMainProduct = isMainProduct
        )
    }
}
