package com.otoki.powersales.order.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.order.entity.ErpOrder
import com.otoki.powersales.order.entity.ErpOrderProduct
import com.otoki.powersales.order.repository.ErpOrderProductRepository
import com.otoki.powersales.order.repository.ErpOrderRepository
import com.otoki.powersales.order.service.dto.ErpOrderLineCommand
import com.otoki.powersales.order.service.dto.ErpOrderUpsertCommand
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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@DisplayName("ErpOrderUpsertService 테스트")
class ErpOrderUpsertServiceTest {

    @Mock
    private lateinit var erpOrderRepository: ErpOrderRepository

    @Mock
    private lateinit var erpOrderProductRepository: ErpOrderProductRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @InjectMocks
    private lateinit var service: ErpOrderUpsertService

    private fun line(
        sapOrderNumber: String? = "0010012345",
        lineNumber: String? = "001",
        productCode: String? = "100100",
        productName: String? = "진라면 매운맛",
        orderQuantity: String? = "100",
        unit: String? = "EA",
        defaultReason: String? = null,
        shippingScheduleTime: String? = null,
        shippingCompleteTime: String? = null,
        shippingVehicle: String? = null
    ): ErpOrderLineCommand = ErpOrderLineCommand(
        sapOrderNumber = sapOrderNumber,
        lineNumber = lineNumber,
        productCode = productCode,
        productName = productName,
        orderQuantity = orderQuantity,
        unit = unit,
        confirmQuantityBox = null,
        confirmQuantity = null,
        confirmUnit = null,
        defaultReason = defaultReason,
        lineItemStatus = null,
        shippingDriverName = null,
        shippingVehicle = shippingVehicle,
        shippingDriverPhone = null,
        shippingScheduleTime = shippingScheduleTime,
        shippingCompleteTime = shippingCompleteTime,
        shippingQuantityBox = null,
        shippingQuantity = null,
        orderSalesLineAmount = null,
        shippingAmount = null,
        plant = null,
        plantNm = null,
        releaseQuantity = null,
        releaseAmount = null
    )

    private fun command(
        sapOrderNumber: String? = "0010012345",
        sapAccountCode: String? = "1032619",
        sapAccountName: String? = "(주)홍길동상회",
        orderSalesAmount: String? = "1500000",
        orderChannel: String? = "01",
        lines: List<ErpOrderLineCommand> = listOf(line())
    ): ErpOrderUpsertCommand = ErpOrderUpsertCommand(
        sapOrderNumber = sapOrderNumber,
        sapAccountCode = sapAccountCode,
        sapAccountName = sapAccountName,
        deliveryRequestDate = null,
        orderDate = null,
        employeeCode = null,
        employeeName = null,
        orderSalesAmount = orderSalesAmount,
        orderChannel = orderChannel,
        orderChannelNm = null,
        orderType = null,
        orderTypeNm = null,
        lines = lines
    )

    private fun account(externalKey: String): Account = Account(externalKey = externalKey)

    private fun mockHeaderSave() {
        whenever(erpOrderRepository.saveAllAndFlush(any<List<ErpOrder>>()))
            .thenAnswer { it.getArgument<List<ErpOrder>>(0) }
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 헤더 1건 + 라인 1건 - 다단 saveAll, headerSuccessCount=1, lineSuccessCount=1")
        fun upsert_insertNewHeaderAndLine() {
            whenever(accountRepository.findByExternalKeyIn(listOf("1032619")))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber("0010012345")).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            val result = service.upsert(listOf(command()))

            val headerCaptor = argumentCaptor<List<ErpOrder>>()
            verify(erpOrderRepository).saveAllAndFlush(headerCaptor.capture())
            assertThat(headerCaptor.firstValue.single().sapOrderNumber).isEqualTo("0010012345")

            val lineCaptor = argumentCaptor<List<ErpOrderProduct>>()
            verify(erpOrderProductRepository).saveAll(lineCaptor.capture())
            val savedLine = lineCaptor.firstValue.single()
            assertThat(savedLine.externalKey).isEqualTo("010012345001")
            assertThat(savedLine.sapOrderNumber).isEqualTo("0010012345")
            assertThat(savedLine.lineNumber).isEqualTo("001")
            assertThat(result.headerSuccessCount).isEqualTo(1)
            assertThat(result.lineSuccessCount).isEqualTo(1)
            assertThat(result.failures).isEmpty()
        }

        @Test
        @DisplayName("기존 헤더 갱신 - 동일 PK 유지, sapAccountName 등 가변 필드 업데이트")
        fun upsert_updateExistingHeader() {
            val existing = ErpOrder(sapOrderNumber = "0010012345").also {
                it.sapAccountName = "이전이름"
                it.orderSalesAmount = BigDecimal.valueOf(0L)
            }
            whenever(accountRepository.findByExternalKeyIn(listOf("1032619")))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber("0010012345")).thenReturn(existing)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            service.upsert(listOf(command(sapAccountName = "새이름", orderSalesAmount = "2000000")))

            val captor = argumentCaptor<List<ErpOrder>>()
            verify(erpOrderRepository).saveAllAndFlush(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.sapAccountName).isEqualTo("새이름")
            assertThat(saved.orderSalesAmount).isEqualByComparingTo(BigDecimal.valueOf(2000000L))
        }

        @Test
        @DisplayName("orderStatus = 결품 - DefaultReason 있고 ShippingScheduleTime 000000")
        fun upsert_deliveryStatusOutOfStock() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>()))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber(any())).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            service.upsert(
                listOf(command(lines = listOf(line(defaultReason = "입고지연", shippingScheduleTime = "000000"))))
            )

            val captor = argumentCaptor<List<ErpOrderProduct>>()
            verify(erpOrderProductRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_OUT_OF_STOCK)
        }

        @Test
        @DisplayName("orderStatus = 배송중 - ShippingScheduleTime 유효 + ShippingCompleteTime 000000")
        fun upsert_deliveryStatusShipping() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>()))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber(any())).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            service.upsert(
                listOf(command(lines = listOf(line(shippingScheduleTime = "143000", shippingCompleteTime = "000000"))))
            )

            val captor = argumentCaptor<List<ErpOrderProduct>>()
            verify(erpOrderProductRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_SHIPPING)
        }

        @Test
        @DisplayName("orderStatus = 배송 완료 - ShippingCompleteTime 유효")
        fun upsert_deliveryStatusDelivered() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>()))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber(any())).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            service.upsert(
                listOf(command(lines = listOf(line(shippingScheduleTime = "143000", shippingCompleteTime = "170000"))))
            )

            val captor = argumentCaptor<List<ErpOrderProduct>>()
            verify(erpOrderProductRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_DELIVERED)
        }

        @Test
        @DisplayName("orderStatus = 대기 - 모든 시간 필드 비어있거나 000000")
        fun upsert_deliveryStatusPending() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>()))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber(any())).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            service.upsert(
                listOf(command(lines = listOf(line(shippingScheduleTime = null, shippingCompleteTime = "000000"))))
            )

            val captor = argumentCaptor<List<ErpOrderProduct>>()
            verify(erpOrderProductRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_PENDING)
            assertThat(saved.shippingScheduleTime).isNull()
            assertThat(saved.shippingCompleteTime).isNull()
        }

        @Test
        @DisplayName("차량 정보 후속 갱신 - 동일 externalKey 로 UPDATE, shippingVehicle 갱신")
        fun upsert_subsequentVehicleUpdate() {
            val existingLine = ErpOrderProduct(
                erpOrder = ErpOrder(sapOrderNumber = "0010012345"),
                sapOrderNumber = "0010012345",
                lineNumber = "001",
                externalKey = "010012345001"
            ).also { it.shippingVehicle = null }

            whenever(accountRepository.findByExternalKeyIn(any<List<String>>()))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber("0010012345")).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey("010012345001"))
                .thenReturn(existingLine)
            mockHeaderSave()

            service.upsert(
                listOf(command(lines = listOf(line(shippingVehicle = "12가3456"))))
            )

            val captor = argumentCaptor<List<ErpOrderProduct>>()
            verify(erpOrderProductRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existingLine)
            assertThat(saved.shippingVehicle).isEqualTo("12가3456")
        }

        @Test
        @DisplayName("externalKey 키 도출 - SAPOrderNumber 선두 0 1자 제거 + LineNumber 결합")
        fun upsert_externalKeyDerivation() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>()))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber(any())).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            service.upsert(
                listOf(command(lines = listOf(line(sapOrderNumber = "0010012345", lineNumber = "002"))))
            )

            val captor = argumentCaptor<List<ErpOrderProduct>>()
            verify(erpOrderProductRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().externalKey).isEqualTo("010012345002")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("Account 매칭 실패 - 헤더 failure, 라인도 미적재")
        fun upsert_accountNotFound() {
            whenever(accountRepository.findByExternalKeyIn(listOf("9999999"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(sapAccountCode = "9999999")))

            assertThat(result.headerSuccessCount).isEqualTo(0)
            assertThat(result.lineSuccessCount).isEqualTo(0)
            assertThat(result.failures.single().identifier).isEqualTo("0010012345")
            assertThat(result.failures.single().reason).isEqualTo("account not found")
            verify(erpOrderRepository, never()).saveAllAndFlush(any<List<ErpOrder>>())
            verify(erpOrderProductRepository, never()).saveAll(any<List<ErpOrderProduct>>())
        }

        @Test
        @DisplayName("SAPOrderNumber 누락 - failures 기록")
        fun upsert_missingOrderNumber() {
            val result = service.upsert(listOf(command(sapOrderNumber = null)))

            assertThat(result.failures.single().reason).contains("SAPOrderNumber 필수")
        }

        @Test
        @DisplayName("SAPAccountCode 누락 - failures 기록")
        fun upsert_missingAccountCode() {
            val result = service.upsert(listOf(command(sapAccountCode = null)))

            assertThat(result.failures.single().reason).contains("SAPAccountCode 필수")
        }

        @Test
        @DisplayName("부분 실패 - 1건 성공 + 1건 Account 매칭 실패")
        fun upsert_partialAccountMatch() {
            whenever(accountRepository.findByExternalKeyIn(listOf("1032619", "9999999")))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber(any())).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()

            val result = service.upsert(
                listOf(
                    command(sapOrderNumber = "0010000001", sapAccountCode = "1032619"),
                    command(sapOrderNumber = "0010000002", sapAccountCode = "9999999")
                )
            )

            assertThat(result.headerSuccessCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("0010000002")
            assertThat(result.failures.single().reason).isEqualTo("account not found")
        }

        @Test
        @DisplayName("라인 ConstraintViolation - 예외 재전파 (트랜잭션 전체 롤백은 @Transactional 책임)")
        fun upsert_lineConstraintViolation() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>()))
                .thenReturn(listOf(account("1032619")))
            whenever(erpOrderRepository.findBySapOrderNumber(any())).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey(any())).thenReturn(null)
            mockHeaderSave()
            whenever(erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()))
                .thenThrow(RuntimeException("constraint violation"))

            assertThatThrownBy { service.upsert(listOf(command())) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("constraint violation")
        }
    }
}
