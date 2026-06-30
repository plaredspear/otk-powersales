package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.service.ErpOrderUpsertService
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderLineCommand
import com.otoki.powersales.domain.activity.order.service.dto.ErpOrderUpsertCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("ErpOrderUpsertService 테스트")
class ErpOrderUpsertServiceTest {

    private val erpOrderRepository: ErpOrderRepository = mockk()
    private val erpOrderProductRepository: ErpOrderProductRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val service = ErpOrderUpsertService(erpOrderRepository, erpOrderProductRepository, accountRepository)

    private fun line(
        sapOrderNumber: String? = "0010012345",
        lineNumber: String? = "001",
        productCode: String? = "100100",
        productName: String? = "진라면 매운맛",
        orderQuantity: String? = "100",
        unit: String? = "EA",
        defaultReason: String? = null,
        lineItemStatus: String? = null,
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
        lineItemStatus = lineItemStatus,
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

    private fun account(externalKey: String, sfid: String? = null): Account =
        Account(externalKey = externalKey, sfid = sfid)

    private fun mockHeaderSave() {
        every { erpOrderRepository.saveAllAndFlush(any<List<ErpOrder>>()) } answers { firstArg<List<ErpOrder>>() }
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 헤더 1건 + 라인 1건 - 다단 saveAll, headerSuccessCount=1, lineSuccessCount=1")
        fun upsert_insertNewHeaderAndLine() {
            every { accountRepository.findByExternalKeyIn(listOf("1032619")) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber("0010012345") } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val headerCaptor = slot<List<ErpOrder>>()
            val lineCaptor = slot<List<ErpOrderProduct>>()
            every { erpOrderRepository.saveAllAndFlush(capture(headerCaptor)) } answers { firstArg<List<ErpOrder>>() }
            every { erpOrderProductRepository.saveAll(capture(lineCaptor)) } answers { firstArg<List<ErpOrderProduct>>() }

            val result = service.upsert(listOf(command()))

            assertThat(headerCaptor.captured.single().sapOrderNumber).isEqualTo("0010012345")
            val savedLine = lineCaptor.captured.single()
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
            every { accountRepository.findByExternalKeyIn(listOf("1032619")) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber("0010012345") } returns existing
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } answers { firstArg<List<ErpOrderProduct>>() }
            val captor = slot<List<ErpOrder>>()
            every { erpOrderRepository.saveAllAndFlush(capture(captor)) } answers { firstArg<List<ErpOrder>>() }

            service.upsert(listOf(command(sapAccountName = "새이름", orderSalesAmount = "2000000")))

            val saved = captor.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.sapAccountName).isEqualTo("새이름")
            assertThat(saved.orderSalesAmount).isEqualByComparingTo(BigDecimal.valueOf(2000000L))
        }

        @Test
        @DisplayName("orderStatus = 결품 - DefaultReason 있고 ShippingScheduleTime 000000")
        fun upsert_deliveryStatusOutOfStock() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(command(lines = listOf(line(defaultReason = "입고지연", shippingScheduleTime = "000000"))))
            )

            assertThat(captor.captured.single().deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_OUT_OF_STOCK)
        }

        @Test
        @DisplayName("orderStatus = 배송중 - ShippingScheduleTime 유효 + ShippingCompleteTime 000000")
        fun upsert_deliveryStatusShipping() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(command(lines = listOf(line(shippingScheduleTime = "143000", shippingCompleteTime = "000000"))))
            )

            assertThat(captor.captured.single().deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_SHIPPING)
        }

        @Test
        @DisplayName("orderStatus = 배송 완료 - ShippingCompleteTime 유효")
        fun upsert_deliveryStatusDelivered() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(command(lines = listOf(line(shippingScheduleTime = "143000", shippingCompleteTime = "170000"))))
            )

            assertThat(captor.captured.single().deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_DELIVERED)
        }

        @Test
        @DisplayName("orderStatus = 대기 - 모든 시간 필드 비어있거나 000000")
        fun upsert_deliveryStatusPending() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(command(lines = listOf(line(shippingScheduleTime = null, shippingCompleteTime = "000000"))))
            )

            val saved = captor.captured.single()
            assertThat(saved.deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_PENDING)
            assertThat(saved.shippingScheduleTime).isNull()
            assertThat(saved.shippingCompleteTime).isNull()
        }

        @Test
        @DisplayName("orderStatus 미설정 - LineItemStatus 만 채워지고 시간/결품 모두 비어있으면 null (레거시 status=='' 동등)")
        fun upsert_deliveryStatusUnsetWhenOnlyLineItemStatus() {
            // 레거시 cls:156 대기 조건은 LineItemStatus 공백도 요구. LineItemStatus 가 채워지면 대기 if 불성립 →
            // 어떤 if 도 안 잡혀 status='' → OrderStatus__c 미설정. 신규 deliveryStatus 도 null 유지여야 한다.
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(
                    command(
                        lines = listOf(
                            line(lineItemStatus = "주문확인", shippingScheduleTime = "000000", shippingCompleteTime = "000000")
                        )
                    )
                )
            )

            assertThat(captor.captured.single().deliveryStatus).isNull()
        }

        @Test
        @DisplayName("orderStatus = 결품 - 배송완료 + 결품 조건 동시 충족 시 결품이 덮어씀 (레거시 last-match-wins)")
        fun upsert_deliveryStatusOutOfStockWinsOverDelivered() {
            // 레거시 cls:158(배송완료) → cls:159(결품) 순서로 평가되어, CompleteTime 이 채워졌어도
            // DefaultReason 설정 + ScheduleTime 미설정이면 마지막 결품 if 가 배송완료를 덮어쓴다.
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(
                    command(
                        lines = listOf(
                            line(defaultReason = "입고지연", shippingScheduleTime = "000000", shippingCompleteTime = "170000")
                        )
                    )
                )
            )

            assertThat(captor.captured.single().deliveryStatus).isEqualTo(ErpOrderUpsertService.STATUS_OUT_OF_STOCK)
        }

        @Test
        @DisplayName("ShippingVehicle 이 externalKey 말미에 포함 - 차량번호 있는 라인은 차량번호 포함 키로 적재 (레거시 키 규격)")
        fun upsert_shippingVehicleAppendedToKey() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber("0010012345") } returns null
            // 차량번호 포함 키로 조회 — 신규 row.
            every { erpOrderProductRepository.findByExternalKey("01001234500112가3456") } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(command(lines = listOf(line(shippingVehicle = "12가3456"))))
            )

            val saved = captor.captured.single()
            // 레거시 IF_REST_SAP_ClientOrderReceive.cls:142 정합 — 키 말미에 ShippingVehicle 덧붙임.
            assertThat(saved.externalKey).isEqualTo("01001234500112가3456")
            assertThat(saved.shippingVehicle).isEqualTo("12가3456")
        }

        @Test
        @DisplayName("Account FK 연결 - SAPAccountCode resolve 거래처를 account + accountSfid 에 연결 (레거시 AccountId__c 정합)")
        fun upsert_linksAccountFk() {
            every { accountRepository.findByExternalKeyIn(listOf("1032619")) } returns
                listOf(account("1032619", sfid = "001A000000ABCDE"))
            every { erpOrderRepository.findBySapOrderNumber("0010012345") } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } answers { firstArg<List<ErpOrderProduct>>() }
            val captor = slot<List<ErpOrder>>()
            every { erpOrderRepository.saveAllAndFlush(capture(captor)) } answers { firstArg<List<ErpOrder>>() }

            service.upsert(listOf(command()))

            val saved = captor.captured.single()
            assertThat(saved.account?.externalKey).isEqualTo("1032619")
            assertThat(saved.accountSfid).isEqualTo("001A000000ABCDE")
        }

        @Test
        @DisplayName("날짜 센티넬 - 빈 OrderDate/DeliveryRequestDate 및 00000000 은 2999-12-31 로 저장 (레거시 convertStringToDate 정합)")
        fun upsert_dateSentinel() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } answers { firstArg<List<ErpOrderProduct>>() }
            val captor = slot<List<ErpOrder>>()
            every { erpOrderRepository.saveAllAndFlush(capture(captor)) } answers { firstArg<List<ErpOrder>>() }

            service.upsert(
                listOf(
                    command().copy(orderDate = null, deliveryRequestDate = "00000000")
                )
            )

            val saved = captor.captured.single()
            val sentinel = java.time.LocalDate.of(2999, 12, 31)
            assertThat(saved.orderDate).isEqualTo(sentinel)
            assertThat(saved.deliveryRequestDate).isEqualTo(sentinel)
        }

        @Test
        @DisplayName("날짜 정상 파싱 - yyyyMMdd 유효 입력은 그대로 LocalDate 변환")
        fun upsert_dateParsedNormally() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } answers { firstArg<List<ErpOrderProduct>>() }
            val captor = slot<List<ErpOrder>>()
            every { erpOrderRepository.saveAllAndFlush(capture(captor)) } answers { firstArg<List<ErpOrder>>() }

            service.upsert(listOf(command().copy(orderDate = "20260619")))

            assertThat(captor.captured.single().orderDate).isEqualTo(java.time.LocalDate.of(2026, 6, 19))
        }

        @Test
        @DisplayName("externalKey 키 도출 - SAPOrderNumber 선두 0 1자 제거 + LineNumber 결합")
        fun upsert_externalKeyDerivation() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val captor = slot<List<ErpOrderProduct>>()
            every { erpOrderProductRepository.saveAll(capture(captor)) } answers { firstArg<List<ErpOrderProduct>>() }

            service.upsert(
                listOf(command(lines = listOf(line(sapOrderNumber = "0010012345", lineNumber = "002"))))
            )

            assertThat(captor.captured.single().externalKey).isEqualTo("010012345002")
        }
    }

    @Nested
    @DisplayName("upsert - 레거시 정합 (수신 필드 명시 필수/거래처 검증 제거)")
    inner class UpsertLegacyAlignment {

        @Test
        @DisplayName("Account 매칭 실패 - 검증 없이 account FK null 로 헤더+라인 적재 (레거시 검증 부재 정합)")
        fun upsert_accountNotFound_storedWithNullFk() {
            every { accountRepository.findByExternalKeyIn(listOf("9999999")) } returns emptyList()
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val headerCaptor = slot<List<ErpOrder>>()
            every { erpOrderRepository.saveAllAndFlush(capture(headerCaptor)) } answers { firstArg<List<ErpOrder>>() }
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } answers { firstArg<List<ErpOrderProduct>>() }

            val result = service.upsert(listOf(command(sapAccountCode = "9999999")))

            assertThat(result.headerSuccessCount).isEqualTo(1)
            assertThat(result.lineSuccessCount).isEqualTo(1)
            assertThat(result.failures).isEmpty()
            val saved = headerCaptor.captured.single()
            // 거래처 미존재 → FK/SFId 미연결, sapAccountCode 원본은 보존 (레거시 raw 적재 정합).
            assertThat(saved.account).isNull()
            assertThat(saved.accountSfid).isNull()
            assertThat(saved.sapAccountCode).isEqualTo("9999999")
        }

        @Test
        @DisplayName("SAPAccountCode 누락 - 검증 없이 account FK null 로 적재")
        fun upsert_missingAccountCode_storedWithNullFk() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val headerCaptor = slot<List<ErpOrder>>()
            every { erpOrderRepository.saveAllAndFlush(capture(headerCaptor)) } answers { firstArg<List<ErpOrder>>() }
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } answers { firstArg<List<ErpOrderProduct>>() }

            val result = service.upsert(listOf(command(sapAccountCode = null)))

            assertThat(result.headerSuccessCount).isEqualTo(1)
            assertThat(result.failures).isEmpty()
            assertThat(headerCaptor.captured.single().account).isNull()
        }

        @Test
        @DisplayName("부분 처리 - 거래처 있는 행/없는 행 모두 적재 (없는 행은 FK null)")
        fun upsert_partialAccountMatch_bothStored() {
            every { accountRepository.findByExternalKeyIn(listOf("1032619", "9999999")) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            val headerCaptor = slot<List<ErpOrder>>()
            every { erpOrderRepository.saveAllAndFlush(capture(headerCaptor)) } answers { firstArg<List<ErpOrder>>() }
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } answers { firstArg<List<ErpOrderProduct>>() }

            val result = service.upsert(
                listOf(
                    command(sapOrderNumber = "0010000001", sapAccountCode = "1032619"),
                    command(sapOrderNumber = "0010000002", sapAccountCode = "9999999")
                )
            )

            assertThat(result.headerSuccessCount).isEqualTo(2)
            assertThat(result.failures).isEmpty()
            val byNumber = headerCaptor.captured.associateBy { it.sapOrderNumber }
            assertThat(byNumber["0010000001"]?.account?.externalKey).isEqualTo("1032619")
            assertThat(byNumber["0010000002"]?.account).isNull()
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("SAPOrderNumber 누락 - 헤더 upsert 키 부재라 failures 기록 (SF Name 키 정합)")
        fun upsert_missingOrderNumber() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))

            val result = service.upsert(listOf(command(sapOrderNumber = null)))

            assertThat(result.headerSuccessCount).isEqualTo(0)
            assertThat(result.failures.single().reason).contains("SAPOrderNumber 필수")
            verify(exactly = 0) { erpOrderRepository.saveAllAndFlush(any<List<ErpOrder>>()) }
        }

        @Test
        @DisplayName("라인 ConstraintViolation - 예외 재전파 (트랜잭션 전체 롤백은 @Transactional 책임)")
        fun upsert_lineConstraintViolation() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns listOf(account("1032619"))
            every { erpOrderRepository.findBySapOrderNumber(any()) } returns null
            every { erpOrderProductRepository.findByExternalKey(any()) } returns null
            mockHeaderSave()
            every { erpOrderProductRepository.saveAll(any<List<ErpOrderProduct>>()) } throws RuntimeException("constraint violation")

            assertThatThrownBy { service.upsert(listOf(command())) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("constraint violation")
        }
    }
}
