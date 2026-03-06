package com.otoki.internal.sap.service

import com.otoki.internal.sap.dto.SapClientOrderRequest.ItemDetail
import com.otoki.internal.sap.dto.SapClientOrderRequest.ReqItem
import com.otoki.internal.sap.entity.ErpOrder
import com.otoki.internal.sap.entity.ErpOrderProduct
import com.otoki.internal.sap.repository.ErpOrderProductRepository
import com.otoki.internal.sap.repository.ErpOrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapClientOrderService 테스트")
class SapClientOrderServiceTest {

    @Mock
    private lateinit var erpOrderRepository: ErpOrderRepository

    @Mock
    private lateinit var erpOrderProductRepository: ErpOrderProductRepository

    @InjectMocks
    private lateinit var sapClientOrderService: SapClientOrderService

    @Nested
    @DisplayName("sync - 신규 주문 동기화")
    inner class NewOrderTests {

        @Test
        @DisplayName("정상 등록 - 1건 헤더 + 상세 3건 -> ErpOrder 1건, ErpOrderProduct 3건 save")
        fun sync_newOrder_createsHeaderAndDetails() {
            val items = listOf(createReqItem(
                sapOrderNumber = "ORD001",
                sapAccountCode = "ACC001",
                sapAccountName = "거래처A",
                orderSalesAmount = "15000.5",
                itemDetailList = listOf(
                    createItemDetail(sapOrderNumber = "ORD001", lineNumber = "10", productCode = "P001"),
                    createItemDetail(sapOrderNumber = "ORD001", lineNumber = "20", productCode = "P002"),
                    createItemDetail(sapOrderNumber = "ORD001", lineNumber = "30", productCode = "P003")
                )
            ))
            whenever(erpOrderRepository.findBySapOrderNumber("ORD001")).thenReturn(null)
            whenever(erpOrderRepository.save(any<ErpOrder>()))
                .thenAnswer { it.getArgument<ErpOrder>(0) }
            whenever(erpOrderProductRepository.findByExternalKey("ORD00110")).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey("ORD00120")).thenReturn(null)
            whenever(erpOrderProductRepository.findByExternalKey("ORD00130")).thenReturn(null)
            whenever(erpOrderProductRepository.save(any<ErpOrderProduct>()))
                .thenAnswer { it.getArgument<ErpOrderProduct>(0) }

            val result = sapClientOrderService.sync(items)

            assertThat(result.failCount).isEqualTo(0)
            val orderCaptor = argumentCaptor<ErpOrder>()
            verify(erpOrderRepository).save(orderCaptor.capture())
            assertThat(orderCaptor.firstValue.sapOrderNumber).isEqualTo("ORD001")
            assertThat(orderCaptor.firstValue.sapAccountCode).isEqualTo("ACC001")
            assertThat(orderCaptor.firstValue.orderSalesAmount).isEqualTo(15000.5)

            val productCaptor = argumentCaptor<ErpOrderProduct>()
            verify(erpOrderProductRepository, times(3)).save(productCaptor.capture())
            assertThat(productCaptor.allValues).hasSize(3)
        }

        @Test
        @DisplayName("order_sales_amount String -> Double 변환 확인")
        fun sync_orderSalesAmount_stringToDouble() {
            val items = listOf(createReqItem(
                sapOrderNumber = "ORD002",
                orderSalesAmount = "99999.99"
            ))
            whenever(erpOrderRepository.findBySapOrderNumber("ORD002")).thenReturn(null)
            whenever(erpOrderRepository.save(any<ErpOrder>()))
                .thenAnswer { it.getArgument<ErpOrder>(0) }

            sapClientOrderService.sync(items)

            val captor = argumentCaptor<ErpOrder>()
            verify(erpOrderRepository).save(captor.capture())
            assertThat(captor.firstValue.orderSalesAmount).isEqualTo(99999.99)
        }
    }

    @Nested
    @DisplayName("sync - 기존 주문 업데이트")
    inner class ExistingOrderTests {

        @Test
        @DisplayName("기존 업데이트 - 필드 변경 및 updatedAt 설정")
        fun sync_existingOrder_updates() {
            val existing = ErpOrder(id = 1, sapOrderNumber = "ORD001")
            existing.sapAccountCode = "OLD_ACC"
            val items = listOf(createReqItem(
                sapOrderNumber = "ORD001",
                sapAccountCode = "NEW_ACC",
                sapAccountName = "새거래처"
            ))
            whenever(erpOrderRepository.findBySapOrderNumber("ORD001")).thenReturn(existing)
            whenever(erpOrderRepository.save(any<ErpOrder>()))
                .thenAnswer { it.getArgument<ErpOrder>(0) }

            sapClientOrderService.sync(items)

            assertThat(existing.sapAccountCode).isEqualTo("NEW_ACC")
            assertThat(existing.sapAccountName).isEqualTo("새거래처")
            assertThat(existing.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("determineDeliveryStatus - 배송상태 판정")
    inner class DeliveryStatusTests {

        @Test
        @DisplayName("defaultReason=null, lineItemStatus=null, scheduleTime=null -> 대기")
        fun deliveryStatus_allNull_returns대기() {
            val detail = createItemDetail()

            val status = sapClientOrderService.determineDeliveryStatus(detail)

            assertThat(status).isEqualTo("대기")
        }

        @Test
        @DisplayName("scheduleTime 있고 completeTime 없음 -> 배송중")
        fun deliveryStatus_scheduleOnly_returns배송중() {
            val detail = createItemDetail(
                shippingScheduleTime = "20260305 14:00"
            )

            val status = sapClientOrderService.determineDeliveryStatus(detail)

            assertThat(status).isEqualTo("배송중")
        }

        @Test
        @DisplayName("completeTime 있음 -> 배송완료")
        fun deliveryStatus_completeTime_returns배송완료() {
            val detail = createItemDetail(
                shippingScheduleTime = "20260305 14:00",
                shippingCompleteTime = "20260305 15:30"
            )

            val status = sapClientOrderService.determineDeliveryStatus(detail)

            assertThat(status).isEqualTo("배송완료")
        }

        @Test
        @DisplayName("defaultReason 있고 scheduleTime 없음 -> 결품")
        fun deliveryStatus_defaultReasonOnly_returns결품() {
            val detail = createItemDetail(
                defaultReason = "품절"
            )

            val status = sapClientOrderService.determineDeliveryStatus(detail)

            assertThat(status).isEqualTo("결품")
        }

        @Test
        @DisplayName("scheduleTime=000000 -> null 처리되어 대기")
        fun deliveryStatus_scheduleTime000000_treatedAsNull() {
            val detail = createItemDetail(
                shippingScheduleTime = "000000"
            )

            val status = sapClientOrderService.determineDeliveryStatus(detail)

            assertThat(status).isEqualTo("대기")
        }

        @Test
        @DisplayName("defaultReason 있고 scheduleTime 있고 completeTime 있음 -> 배송완료 (condition 3 wins)")
        fun deliveryStatus_allPresent_returns배송완료() {
            val detail = createItemDetail(
                defaultReason = "품절",
                shippingScheduleTime = "20260305 14:00",
                shippingCompleteTime = "20260305 15:30"
            )

            val status = sapClientOrderService.determineDeliveryStatus(detail)

            assertThat(status).isEqualTo("배송완료")
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("sap_order_number null -> 해당 헤더 실패, 나머지 성공")
        fun sync_nullSapOrderNumber_partialFailure() {
            val items = listOf(
                createReqItem(sapOrderNumber = null),
                createReqItem(sapOrderNumber = "ORD002", sapAccountCode = "ACC002")
            )
            whenever(erpOrderRepository.findBySapOrderNumber("ORD002")).thenReturn(null)
            whenever(erpOrderRepository.save(any<ErpOrder>()))
                .thenAnswer { it.getArgument<ErpOrder>(0) }

            val result = sapClientOrderService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].field).isEqualTo("sap_order_number")
        }

        @Test
        @DisplayName("line_number null in detail -> 해당 주문 전체 실패")
        fun sync_nullLineNumber_orderFails() {
            val items = listOf(createReqItem(
                sapOrderNumber = "ORD001",
                itemDetailList = listOf(
                    createItemDetail(sapOrderNumber = "ORD001", lineNumber = null)
                )
            ))
            whenever(erpOrderRepository.findBySapOrderNumber("ORD001")).thenReturn(null)
            whenever(erpOrderRepository.save(any<ErpOrder>()))
                .thenAnswer { it.getArgument<ErpOrder>(0) }

            val result = sapClientOrderService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("line_number")
        }
    }

    @Nested
    @DisplayName("sync - external_key 생성")
    inner class ExternalKeyTests {

        @Test
        @DisplayName("shippingVehicle 있으면 sapOrderNumber + lineNumber + shippingVehicle")
        fun externalKey_withShippingVehicle() {
            val items = listOf(createReqItem(
                sapOrderNumber = "ORD001",
                itemDetailList = listOf(
                    createItemDetail(
                        sapOrderNumber = "ORD001",
                        lineNumber = "10",
                        shippingVehicle = "VH01"
                    )
                )
            ))
            whenever(erpOrderRepository.findBySapOrderNumber("ORD001")).thenReturn(null)
            whenever(erpOrderRepository.save(any<ErpOrder>()))
                .thenAnswer { it.getArgument<ErpOrder>(0) }
            whenever(erpOrderProductRepository.findByExternalKey("ORD00110VH01")).thenReturn(null)
            whenever(erpOrderProductRepository.save(any<ErpOrderProduct>()))
                .thenAnswer { it.getArgument<ErpOrderProduct>(0) }

            sapClientOrderService.sync(items)

            val captor = argumentCaptor<ErpOrderProduct>()
            verify(erpOrderProductRepository).save(captor.capture())
            assertThat(captor.firstValue.externalKey).isEqualTo("ORD00110VH01")
        }

        @Test
        @DisplayName("shippingVehicle 없으면 sapOrderNumber + lineNumber")
        fun externalKey_withoutShippingVehicle() {
            val items = listOf(createReqItem(
                sapOrderNumber = "ORD001",
                itemDetailList = listOf(
                    createItemDetail(sapOrderNumber = "ORD001", lineNumber = "10")
                )
            ))
            whenever(erpOrderRepository.findBySapOrderNumber("ORD001")).thenReturn(null)
            whenever(erpOrderRepository.save(any<ErpOrder>()))
                .thenAnswer { it.getArgument<ErpOrder>(0) }
            whenever(erpOrderProductRepository.findByExternalKey("ORD00110")).thenReturn(null)
            whenever(erpOrderProductRepository.save(any<ErpOrderProduct>()))
                .thenAnswer { it.getArgument<ErpOrderProduct>(0) }

            sapClientOrderService.sync(items)

            val captor = argumentCaptor<ErpOrderProduct>()
            verify(erpOrderProductRepository).save(captor.capture())
            assertThat(captor.firstValue.externalKey).isEqualTo("ORD00110")
        }
    }

    private fun createReqItem(
        sapOrderNumber: String? = null,
        sapAccountCode: String? = null,
        sapAccountName: String? = null,
        deliveryRequestDate: String? = null,
        orderDate: String? = null,
        employeeCode: String? = null,
        employeeName: String? = null,
        orderSalesAmount: String? = null,
        orderChannel: String? = null,
        orderChannelNm: String? = null,
        orderType: String? = null,
        orderTypeNm: String? = null,
        itemDetailList: List<ItemDetail> = emptyList()
    ) = ReqItem(
        sapOrderNumber = sapOrderNumber,
        sapAccountCode = sapAccountCode,
        sapAccountName = sapAccountName,
        deliveryRequestDate = deliveryRequestDate,
        orderDate = orderDate,
        employeeCode = employeeCode,
        employeeName = employeeName,
        orderSalesAmount = orderSalesAmount,
        orderChannel = orderChannel,
        orderChannelNm = orderChannelNm,
        orderType = orderType,
        orderTypeNm = orderTypeNm,
        itemDetailList = itemDetailList
    )

    private fun createItemDetail(
        sapOrderNumber: String? = null,
        lineNumber: String? = null,
        productCode: String? = null,
        productName: String? = null,
        orderQuantity: String? = null,
        unit: String? = null,
        confirmQuantityBox: String? = null,
        confirmQuantity: String? = null,
        confirmUnit: String? = null,
        defaultReason: String? = null,
        lineItemStatus: String? = null,
        shippingDriverName: String? = null,
        shippingVehicle: String? = null,
        shippingDriverPhone: String? = null,
        shippingScheduleTime: String? = null,
        shippingCompleteTime: String? = null,
        shippingQuantityBox: String? = null,
        shippingQuantity: String? = null,
        orderSalesLineAmount: String? = null,
        shippingAmount: String? = null,
        plant: String? = null,
        plantNm: String? = null,
        releaseQuantity: String? = null,
        releaseAmount: String? = null
    ) = ItemDetail(
        sapOrderNumber = sapOrderNumber,
        lineNumber = lineNumber,
        productCode = productCode,
        productName = productName,
        orderQuantity = orderQuantity,
        unit = unit,
        confirmQuantityBox = confirmQuantityBox,
        confirmQuantity = confirmQuantity,
        confirmUnit = confirmUnit,
        defaultReason = defaultReason,
        lineItemStatus = lineItemStatus,
        shippingDriverName = shippingDriverName,
        shippingVehicle = shippingVehicle,
        shippingDriverPhone = shippingDriverPhone,
        shippingScheduleTime = shippingScheduleTime,
        shippingCompleteTime = shippingCompleteTime,
        shippingQuantityBox = shippingQuantityBox,
        shippingQuantity = shippingQuantity,
        orderSalesLineAmount = orderSalesLineAmount,
        shippingAmount = shippingAmount,
        plant = plant,
        plantNm = plantNm,
        releaseQuantity = releaseQuantity,
        releaseAmount = releaseAmount
    )
}
