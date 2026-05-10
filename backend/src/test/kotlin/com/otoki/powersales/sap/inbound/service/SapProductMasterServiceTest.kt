package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.product.service.ProductUpsertService
import com.otoki.powersales.product.service.dto.ProductUpsertCommand
import com.otoki.powersales.product.service.dto.ProductUpsertFailedRow
import com.otoki.powersales.product.service.dto.ProductUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterRequestItem
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapProductMasterService 어댑터 테스트")
class SapProductMasterServiceTest {

    @Mock
    private lateinit var productUpsertService: ProductUpsertService

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapProductMasterService

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 매핑 + audit reason='success=N failure=0'")
        fun happy_domainResultMappedAndAudit() {
            val items = listOf(
                ProductMasterRequestItem(productCode = "100100", productName = "진라면", standardPrice = "4500")
            )
            whenever(productUpsertService.upsert(any())).thenReturn(
                ProductUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditCaptor.firstValue.receivedCount).isEqualTo(1)
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=1 failure=0")
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                ProductMasterRequestItem(productCode = "100100", productName = "진라면"),
                ProductMasterRequestItem(productCode = "100200", productName = "안성탕면", standardPrice = "abc")
            )
            whenever(productUpsertService.upsert(any())).thenReturn(
                ProductUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(ProductUpsertFailedRow("100200", "StandardPrice 변환 실패: abc"))
                )
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("100200")
            assertThat(detail.failures.single().reason).isEqualTo("StandardPrice 변환 실패: abc")
        }

        @Test
        @DisplayName("도메인 throw: 실패 audit 후 예외 재전파")
        fun domainThrow_failureAuditAndRethrow() {
            val items = listOf(ProductMasterRequestItem(productCode = "100100", productName = "진라면"))
            whenever(productUpsertService.upsert(any()))
                .thenThrow(IllegalStateException("DB connection lost"))

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=0 failure=1")
        }

        @Test
        @DisplayName("DTO 매핑: ProductMasterRequestItem → ProductUpsertCommand 필드 매핑")
        fun dtoMapping_itemToCommand() {
            val items = listOf(
                ProductMasterRequestItem(
                    productCode = "100100",
                    productName = "진라면",
                    standardPrice = "4500",
                    launchDate = "20200101",
                    storeCondition = "냉장보관",
                    productBarcode = "8801007123456",
                    pallet = "100"
                )
            )
            whenever(productUpsertService.upsert(any())).thenReturn(
                ProductUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            service.upsert(items)

            val captor = argumentCaptor<List<ProductUpsertCommand>>()
            verify(productUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.productCode).isEqualTo("100100")
            assertThat(command.productName).isEqualTo("진라면")
            assertThat(command.standardPrice).isEqualTo("4500")
            assertThat(command.launchDate).isEqualTo("20200101")
            assertThat(command.storeCondition).isEqualTo("냉장보관")
            assertThat(command.productBarcode).isEqualTo("8801007123456")
            assertThat(command.pallet).isEqualTo("100")
        }
    }
}
