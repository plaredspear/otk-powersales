package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.product.service.ProductUpsertService
import com.otoki.powersales.product.service.dto.ProductUpsertCommand
import com.otoki.powersales.product.service.dto.ProductUpsertFailedRow
import com.otoki.powersales.product.service.dto.ProductUpsertResult
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterRequestItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SapProductMasterService 어댑터 테스트")
class SapProductMasterServiceTest {

    private val productUpsertService: ProductUpsertService = mockk()
    private val service = SapProductMasterService(productUpsertService)

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 매핑")
        fun happy_domainResultMapped() {
            val items = listOf(
                ProductMasterRequestItem(productCode = "100100", productName = "진라면", standardPrice = "4500")
            )
            every { productUpsertService.upsert(any()) } returns
                ProductUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑")
        fun partialFailure_failureRowsMapped() {
            val items = listOf(
                ProductMasterRequestItem(productCode = "100100", productName = "진라면"),
                ProductMasterRequestItem(productCode = "100200", productName = "안성탕면", standardPrice = "abc")
            )
            every { productUpsertService.upsert(any()) } returns
                ProductUpsertResult(
                    successCount = 1,
                    failureCount = 1,
                    failures = listOf(ProductUpsertFailedRow("100200", "StandardPrice 변환 실패: abc"))
                )

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("100200")
            assertThat(detail.failures.single().reason).isEqualTo("StandardPrice 변환 실패: abc")
        }

        @Test
        @DisplayName("도메인 throw: 어댑터는 catch 하지 않고 그대로 재전파 (audit 은 Aspect 책임)")
        fun domainThrow_propagated() {
            val items = listOf(ProductMasterRequestItem(productCode = "100100", productName = "진라면"))
            every { productUpsertService.upsert(any()) } throws IllegalStateException("DB connection lost")

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(IllegalStateException::class.java)
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
            every { productUpsertService.upsert(any()) } returns
                ProductUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())

            service.upsert(items)

            val captor = slot<List<ProductUpsertCommand>>()
            verify { productUpsertService.upsert(capture(captor)) }
            val command = captor.captured.single()
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
