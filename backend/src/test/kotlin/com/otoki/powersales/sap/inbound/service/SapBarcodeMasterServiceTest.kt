package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.product.service.ProductBarcodeUpsertService
import com.otoki.powersales.product.service.dto.ProductBarcodeUpsertCommand
import com.otoki.powersales.product.service.dto.ProductBarcodeUpsertFailedRow
import com.otoki.powersales.product.service.dto.ProductBarcodeUpsertResult
import com.otoki.powersales.sap.inbound.dto.product.BarcodeMasterRequestItem
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

/**
 * Spec #639: REQUEST_ACCEPTED audit 검증은 SapInboundAuditAspectTest 가 책임.
 * 본 테스트는 어댑터의 도메인 호출 / DTO 매핑 / 응답 매핑만 검증.
 */
@ExtendWith(MockitoExtension::class)
@DisplayName("SapBarcodeMasterService 어댑터 테스트")
class SapBarcodeMasterServiceTest {

    @Mock
    private lateinit var productBarcodeUpsertService: ProductBarcodeUpsertService

    @InjectMocks
    private lateinit var service: SapBarcodeMasterService

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (success=1, failure=0) → ProductMasterDetail")
        fun happy_domainResultMapped() {
            val items = listOf(
                BarcodeMasterRequestItem(
                    productCode = "100100",
                    productName = "진라면 매운맛 5입",
                    productUnit = "EA",
                    productSequence = "001",
                    productBarcode = "8801045123456"
                )
            )
            whenever(productBarcodeUpsertService.upsert(any())).thenReturn(
                ProductBarcodeUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 으로 1:1 매핑")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                BarcodeMasterRequestItem(productCode = "100100", productUnit = "EA", productSequence = "001", productBarcode = "111"),
                BarcodeMasterRequestItem(productCode = "999999", productUnit = "EA", productSequence = "001", productBarcode = "222")
            )
            whenever(productBarcodeUpsertService.upsert(any())).thenReturn(
                ProductBarcodeUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(ProductBarcodeUpsertFailedRow("999999EA001", "product_code not found: 999999"))
                )
            )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("999999EA001")
            assertThat(detail.failures.single().reason).isEqualTo("product_code not found: 999999")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파 (audit 은 Aspect 책임)")
        fun domainThrow_propagated() {
            val items = listOf(
                BarcodeMasterRequestItem(productCode = "100100", productUnit = "EA", productSequence = "001", productBarcode = "111")
            )
            whenever(productBarcodeUpsertService.upsert(any()))
                .thenThrow(IllegalStateException("DB connection lost"))

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        @DisplayName("DTO 매핑: BarcodeMasterRequestItem → ProductBarcodeUpsertCommand 필드 매핑")
        fun dtoMapping_itemToCommand() {
            val items = listOf(
                BarcodeMasterRequestItem(
                    productCode = "100100",
                    productName = "진라면",
                    productUnit = "EA",
                    productSequence = "001",
                    productBarcode = "8801045123456"
                )
            )
            whenever(productBarcodeUpsertService.upsert(any())).thenReturn(
                ProductBarcodeUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            service.upsert(items)

            val captor = argumentCaptor<List<ProductBarcodeUpsertCommand>>()
            verify(productBarcodeUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.productCode).isEqualTo("100100")
            assertThat(command.productName).isEqualTo("진라면")
            assertThat(command.productUnit).isEqualTo("EA")
            assertThat(command.productSequence).isEqualTo("001")
            assertThat(command.productBarcode).isEqualTo("8801045123456")
        }
    }
}
