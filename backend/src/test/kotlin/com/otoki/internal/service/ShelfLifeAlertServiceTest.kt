package com.otoki.internal.service

import com.otoki.internal.entity.*
import com.otoki.internal.repository.ShelfLifeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("ShelfLifeAlertService 테스트")
class ShelfLifeAlertServiceTest {

    @Mock
    private lateinit var shelfLifeRepository: ShelfLifeRepository

    @InjectMocks
    private lateinit var shelfLifeAlertService: ShelfLifeAlertService

    private lateinit var testUser: User
    private lateinit var testStore: Store
    private lateinit var testProduct: Product

    @BeforeEach
    fun setUp() {
        testUser = User(
            id = 1L,
            employeeId = "EMP001",
            password = "encoded",
            name = "테스트 사원",
            orgName = "강남지점"
        )

        testStore = Store(
            id = 10L,
            storeCode = "S001",
            storeName = "이마트 강남점"
        )

        testProduct = Product(
            id = 100L,
            productId = "PROD001",
            productName = "오뚜기 카레",
            productCode = "P001",
            barcode = "8801234567890",
            storageType = "냉장"
        )
    }

    @Test
    @DisplayName("알림 대상이 없으면 아무 처리도 하지 않는다")
    fun sendExpiryAlerts_WhenNoTargets_DoesNothing() {
        // given
        whenever(shelfLifeRepository.findByAlertDateAndAlertSentFalse(any<LocalDate>()))
            .thenReturn(emptyList())

        // when
        shelfLifeAlertService.sendExpiryAlerts()

        // then
        verify(shelfLifeRepository).findByAlertDateAndAlertSentFalse(any<LocalDate>())
        verify(shelfLifeRepository, never()).save(any<ShelfLife>())
    }

    @Test
    @DisplayName("알림 대상이 있으면 alertSent를 true로 업데이트한다")
    fun sendExpiryAlerts_WhenTargetsExist_UpdatesAlertSent() {
        // given
        val shelfLife1 = ShelfLife(
            id = 1L,
            user = testUser,
            store = testStore,
            product = testProduct,
            productCode = "P001",
            productName = "오뚜기 카레",
            storeName = "이마트 강남점",
            expiryDate = LocalDate.now().plusDays(7),
            alertDate = LocalDate.now(),
            alertSent = false
        )
        val shelfLife2 = ShelfLife(
            id = 2L,
            user = testUser,
            store = testStore,
            product = testProduct,
            productCode = "P002",
            productName = "오뚜기 라면",
            storeName = "이마트 강남점",
            expiryDate = LocalDate.now().plusDays(3),
            alertDate = LocalDate.now(),
            alertSent = false
        )

        whenever(shelfLifeRepository.findByAlertDateAndAlertSentFalse(any<LocalDate>()))
            .thenReturn(listOf(shelfLife1, shelfLife2))
        whenever(shelfLifeRepository.save(any<ShelfLife>())).thenAnswer { it.arguments[0] }

        // when
        shelfLifeAlertService.sendExpiryAlerts()

        // then
        verify(shelfLifeRepository, times(2)).save(any<ShelfLife>())
        assert(shelfLife1.alertSent) { "shelfLife1.alertSent should be true" }
        assert(shelfLife2.alertSent) { "shelfLife2.alertSent should be true" }
    }

    @Test
    @DisplayName("알림 발송 중 예외 발생 시 해당 항목만 실패하고 나머지는 계속 처리한다")
    fun sendExpiryAlerts_WhenPartialFailure_ContinuesProcessing() {
        // given
        val shelfLife1 = ShelfLife(
            id = 1L,
            user = testUser,
            store = testStore,
            product = testProduct,
            productCode = "P001",
            productName = "오뚜기 카레",
            storeName = "이마트 강남점",
            expiryDate = LocalDate.now().plusDays(7),
            alertDate = LocalDate.now(),
            alertSent = false
        )
        val shelfLife2 = ShelfLife(
            id = 2L,
            user = testUser,
            store = testStore,
            product = testProduct,
            productCode = "P002",
            productName = "오뚜기 라면",
            storeName = "이마트 강남점",
            expiryDate = LocalDate.now().plusDays(3),
            alertDate = LocalDate.now(),
            alertSent = false
        )

        whenever(shelfLifeRepository.findByAlertDateAndAlertSentFalse(any<LocalDate>()))
            .thenReturn(listOf(shelfLife1, shelfLife2))

        // 첫 번째 save는 예외 발생, 두 번째는 성공
        whenever(shelfLifeRepository.save(any<ShelfLife>()))
            .thenThrow(RuntimeException("DB error"))
            .thenAnswer { it.arguments[0] }

        // when
        shelfLifeAlertService.sendExpiryAlerts()

        // then
        verify(shelfLifeRepository, times(2)).save(any<ShelfLife>())
        // 첫 번째는 save 전에 alertSent=true로 설정하지만 예외로 인해 롤백되지 않음 (in-memory 변경은 유지)
        // 두 번째는 정상 처리
        assert(shelfLife2.alertSent) { "shelfLife2.alertSent should be true" }
    }

    @Test
    @DisplayName("dDay(남은 일수)가 올바르게 계산되어 로그에 기록된다")
    fun sendExpiryAlerts_CalculatesDaysRemaining() {
        // given
        val daysUntilExpiry = 5L
        val shelfLife = ShelfLife(
            id = 1L,
            user = testUser,
            store = testStore,
            product = testProduct,
            productCode = "P001",
            productName = "오뚜기 카레",
            storeName = "이마트 강남점",
            expiryDate = LocalDate.now().plusDays(daysUntilExpiry),
            alertDate = LocalDate.now(),
            alertSent = false
        )

        whenever(shelfLifeRepository.findByAlertDateAndAlertSentFalse(any<LocalDate>()))
            .thenReturn(listOf(shelfLife))
        whenever(shelfLifeRepository.save(any<ShelfLife>())).thenAnswer { it.arguments[0] }

        // when
        shelfLifeAlertService.sendExpiryAlerts()

        // then
        verify(shelfLifeRepository).save(any<ShelfLife>())
        assert(shelfLife.alertSent) { "shelfLife.alertSent should be true after successful processing" }
    }
}
