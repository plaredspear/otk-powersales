package com.otoki.internal.service

import com.otoki.internal.dto.request.DailySalesCreateRequest
import com.otoki.internal.entity.DailySales
import com.otoki.internal.entity.Event
import com.otoki.internal.exception.*
import com.otoki.internal.repository.DailySalesRepository
import com.otoki.internal.repository.EventProductRepository
import com.otoki.internal.repository.EventRepository
import com.otoki.internal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("DailySalesService 테스트")
class DailySalesServiceTest {

    @Mock
    private lateinit var dailySalesRepository: DailySalesRepository

    @Mock
    private lateinit var eventRepository: EventRepository

    @Mock
    private lateinit var eventProductRepository: EventProductRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var fileStorageService: FileStorageService

    @InjectMocks
    private lateinit var dailySalesService: DailySalesService

    private val testUserId = 1L
    private val testEmployeeId = "12345"
    private val testEventId = "EVT001"
    private val today = LocalDate.now()

    private fun createTestUser(): com.otoki.internal.entity.User {
        return com.otoki.internal.entity.User(
            id = testUserId,
            employeeId = testEmployeeId,
            password = "encoded",
            name = "테스트",
            department = "영업팀",
            branchName = "서울"
        )
    }

    private fun createTestEvent(): Event {
        return Event(
            id = 1L,
            eventId = testEventId,
            eventType = "프로모션",
            eventName = "진라면 행사",
            startDate = today.minusDays(5),
            endDate = today.plusDays(10),
            customerId = "CUST001",
            assigneeId = testEmployeeId,
            targetAmount = 1000000
        )
    }

    private fun createTestPhoto(): MockMultipartFile {
        return MockMultipartFile(
            "photo",
            "test.jpg",
            "image/jpeg",
            "test image content".toByteArray()
        )
    }

    @Test
    @DisplayName("일매출 등록 성공 - 대표제품만 입력")
    fun registerDailySales_Success_MainProductOnly() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val photo = createTestPhoto()
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            photo = photo
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(false)
        whenever(fileStorageService.uploadDailySalesPhoto(any(), any(), any(), any()))
            .thenReturn("https://example.com/photo.jpg")
        whenever(dailySalesRepository.save(any<DailySales>())).thenAnswer { invocation ->
            val arg = invocation.arguments[0] as DailySales
            DailySales(
                id = 123L,
                eventId = arg.eventId,
                employeeId = arg.employeeId,
                salesDate = arg.salesDate,
                mainProductPrice = arg.mainProductPrice,
                mainProductQuantity = arg.mainProductQuantity,
                mainProductAmount = arg.mainProductAmount,
                subProductCode = arg.subProductCode,
                subProductQuantity = arg.subProductQuantity,
                subProductAmount = arg.subProductAmount,
                photoUrl = arg.photoUrl,
                status = arg.status
            )
        }

        // When
        val response = dailySalesService.registerDailySales(
            userId = testUserId,
            eventId = testEventId,
            request = request
        )

        // Then
        assertThat(response.dailySalesId).isEqualTo("123")
        assertThat(response.status).isEqualTo(DailySales.STATUS_REGISTERED)
        assertThat(response.totalAmount).isEqualTo(10000)
        verify(dailySalesRepository).save(any<DailySales>())
    }

    @Test
    @DisplayName("일매출 등록 성공 - 기타제품만 입력")
    fun registerDailySales_Success_SubProductOnly() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val photo = createTestPhoto()
        val request = DailySalesCreateRequest(
            subProductCode = "SUB001",
            subProductQuantity = 5,
            subProductAmount = 25000,
            photo = photo
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(false)
        whenever(eventProductRepository.existsByEventIdAndProductCode(testEventId, "SUB001"))
            .thenReturn(true)
        whenever(fileStorageService.uploadDailySalesPhoto(any(), any(), any(), any()))
            .thenReturn("https://example.com/photo.jpg")
        whenever(dailySalesRepository.save(any<DailySales>())).thenAnswer { invocation ->
            val arg = invocation.arguments[0] as DailySales
            DailySales(
                id = 456L,
                eventId = arg.eventId,
                employeeId = arg.employeeId,
                salesDate = arg.salesDate,
                mainProductPrice = arg.mainProductPrice,
                mainProductQuantity = arg.mainProductQuantity,
                mainProductAmount = arg.mainProductAmount,
                subProductCode = arg.subProductCode,
                subProductQuantity = arg.subProductQuantity,
                subProductAmount = arg.subProductAmount,
                photoUrl = arg.photoUrl,
                status = arg.status
            )
        }

        // When
        val response = dailySalesService.registerDailySales(
            userId = testUserId,
            eventId = testEventId,
            request = request
        )

        // Then
        assertThat(response.dailySalesId).isEqualTo("456")
        assertThat(response.status).isEqualTo(DailySales.STATUS_REGISTERED)
        assertThat(response.totalAmount).isEqualTo(25000)
    }

    @Test
    @DisplayName("일매출 등록 실패 - 행사를 찾을 수 없음")
    fun registerDailySales_Failure_EventNotFound() {
        // Given
        val user = createTestUser()
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            photo = createTestPhoto()
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(EventNotFoundException::class.java)
    }

    @Test
    @DisplayName("일매출 등록 실패 - 담당자가 아님")
    fun registerDailySales_Failure_NotAssignee() {
        // Given
        val otherUser = createTestUser().apply {
            val userWithDifferentId = com.otoki.internal.entity.User(
                id = testUserId,
                employeeId = "99999",
                password = "encoded",
                name = "다른사람",
                department = "영업팀",
                branchName = "서울"
            )
        }
        val event = createTestEvent()
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            photo = createTestPhoto()
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(
            com.otoki.internal.entity.User(
                id = testUserId,
                employeeId = "99999",
                password = "encoded",
                name = "다른사람",
                department = "영업팀",
                branchName = "서울"
            )
        ))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(DailySalesForbiddenException::class.java)
    }

    @Test
    @DisplayName("일매출 등록 실패 - 행사 기간이 아님")
    fun registerDailySales_Failure_EventPeriodExpired() {
        // Given
        val user = createTestUser()
        val expiredEvent = Event(
            id = 1L,
            eventId = testEventId,
            eventType = "프로모션",
            eventName = "진라면 행사",
            startDate = today.minusDays(30),
            endDate = today.minusDays(1),
            customerId = "CUST001",
            assigneeId = testEmployeeId,
            targetAmount = 1000000
        )
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            photo = createTestPhoto()
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(expiredEvent))

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(EventPeriodExpiredException::class.java)
    }

    @Test
    @DisplayName("일매출 등록 실패 - 이미 등록됨")
    fun registerDailySales_Failure_AlreadyRegistered() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10,
            photo = createTestPhoto()
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(true)

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(DailySalesAlreadyRegisteredException::class.java)
    }

    @Test
    @DisplayName("일매출 등록 실패 - 대표제품과 기타제품 모두 미입력")
    fun registerDailySales_Failure_NoProductInput() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val request = DailySalesCreateRequest(
            photo = createTestPhoto()
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(false)

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(DailySalesInvalidParameterException::class.java)
            .hasMessageContaining("최소 하나를 입력")
    }

    @Test
    @DisplayName("일매출 등록 실패 - 기타제품 부분 입력")
    fun registerDailySales_Failure_PartialSubProduct() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val request = DailySalesCreateRequest(
            subProductCode = "SUB001",
            subProductQuantity = 5,
            photo = createTestPhoto()
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(false)

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(DailySalesInvalidParameterException::class.java)
            .hasMessageContaining("모두 입력")
    }

    @Test
    @DisplayName("일매출 등록 실패 - 사진 미첨부")
    fun registerDailySales_Failure_NoPhoto() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val request = DailySalesCreateRequest(
            mainProductPrice = 1000,
            mainProductQuantity = 10
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(false)

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(DailySalesInvalidPhotoException::class.java)
    }

    @Test
    @DisplayName("일매출 등록 실패 - 유효하지 않은 제품 코드")
    fun registerDailySales_Failure_InvalidProductCode() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val request = DailySalesCreateRequest(
            subProductCode = "INVALID",
            subProductQuantity = 5,
            subProductAmount = 25000,
            photo = createTestPhoto()
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.existsByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(false)
        whenever(eventProductRepository.existsByEventIdAndProductCode(testEventId, "INVALID"))
            .thenReturn(false)

        // When & Then
        assertThatThrownBy {
            dailySalesService.registerDailySales(testUserId, testEventId, request)
        }.isInstanceOf(DailySalesInvalidProductException::class.java)
    }

    @Test
    @DisplayName("임시저장 성공 - 신규 생성")
    fun saveDailySalesDraft_Success_Create() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val request = DailySalesCreateRequest(
            mainProductPrice = 500
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.findByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(Optional.empty())
        whenever(dailySalesRepository.save(any<DailySales>())).thenAnswer { invocation ->
            val arg = invocation.arguments[0] as DailySales
            DailySales(
                id = 789L,
                eventId = arg.eventId,
                employeeId = arg.employeeId,
                salesDate = arg.salesDate,
                mainProductPrice = arg.mainProductPrice,
                mainProductQuantity = arg.mainProductQuantity,
                mainProductAmount = arg.mainProductAmount,
                subProductCode = arg.subProductCode,
                subProductQuantity = arg.subProductQuantity,
                subProductAmount = arg.subProductAmount,
                photoUrl = arg.photoUrl,
                status = arg.status
            )
        }

        // When
        val response = dailySalesService.saveDailySalesDraft(
            userId = testUserId,
            eventId = testEventId,
            request = request
        )

        // Then
        assertThat(response.dailySalesId).isEqualTo("789")
        assertThat(response.status).isEqualTo(DailySales.STATUS_DRAFT)
        verify(dailySalesRepository).save(any<DailySales>())
    }

    @Test
    @DisplayName("임시저장 성공 - 기존 DRAFT 업데이트")
    fun saveDailySalesDraft_Success_Update() {
        // Given
        val user = createTestUser()
        val event = createTestEvent()
        val existingDraft = DailySales(
            id = 999L,
            eventId = testEventId,
            employeeId = testEmployeeId,
            salesDate = today,
            mainProductPrice = 300,
            status = DailySales.STATUS_DRAFT
        )
        val request = DailySalesCreateRequest(
            mainProductPrice = 500,
            mainProductQuantity = 10
        )

        whenever(userRepository.findById(testUserId)).thenReturn(Optional.of(user))
        whenever(eventRepository.findByEventId(testEventId)).thenReturn(Optional.of(event))
        whenever(dailySalesRepository.findByEventIdAndEmployeeIdAndSalesDateAndStatus(
            any(), any(), any(), any()
        )).thenReturn(Optional.of(existingDraft))
        whenever(dailySalesRepository.save(any<DailySales>())).thenAnswer { invocation ->
            invocation.arguments[0] as DailySales
        }

        // When
        val response = dailySalesService.saveDailySalesDraft(
            userId = testUserId,
            eventId = testEventId,
            request = request
        )

        // Then
        assertThat(response.dailySalesId).isEqualTo("999")
        assertThat(response.status).isEqualTo(DailySales.STATUS_DRAFT)
        verify(dailySalesRepository).save(any<DailySales>())
    }
}
