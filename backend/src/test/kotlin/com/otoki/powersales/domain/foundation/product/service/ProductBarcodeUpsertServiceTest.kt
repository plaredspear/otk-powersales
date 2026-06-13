package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.service.ProductBarcodeUpsertService
import com.otoki.powersales.domain.foundation.product.repository.ProductBarcodeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertCommand
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProductBarcodeUpsertService 테스트")
class ProductBarcodeUpsertServiceTest {

    private val productBarcodeRepository: ProductBarcodeRepository = mockk()
    private val productRepository: ProductRepository = mockk()

    private val service = ProductBarcodeUpsertService(
        productBarcodeRepository,
        productRepository,
    )

    private fun command(
        productCode: String? = "100100",
        productName: String? = "진라면 매운맛 5입",
        productUnit: String? = "EA",
        productSequence: String? = "001",
        productBarcode: String? = "8801045123456"
    ): ProductBarcodeUpsertCommand = ProductBarcodeUpsertCommand(
        productCode = productCode,
        productName = productName,
        productUnit = productUnit,
        productSequence = productSequence,
        productBarcode = productBarcode
    )

    private fun stubSaveAllCapture(): CapturingSlot<List<ProductBarcode>> {
        val slot = slot<List<ProductBarcode>>()
        every { productBarcodeRepository.saveAll(capture(slot)) } answers { firstArg<List<ProductBarcode>>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, FK 매핑, name=ProductUnit")
        fun upsert_insertNew() {
            val product = Product(id = 7L, productCode = "100100", name = "진라면 매운맛 5입")
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns listOf(product)
            every { productBarcodeRepository.findByCustomKey("100100EA001") } returns null
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command()))

            val saved = savedSlot.captured.single()
            assertThat(saved.customKey).isEqualTo("100100EA001")
            assertThat(saved.name).isEqualTo("EA")
            assertThat(saved.unit).isEqualTo("EA")
            assertThat(saved.barcode).isEqualTo("8801045123456")
            assertThat(saved.sortOrder).isEqualTo("001")
            assertThat(saved.productId).isEqualTo(7L)
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("기존 customKey - UPDATE, barcode 변경")
        fun upsert_updateExisting() {
            val product = Product(id = 7L, productCode = "100100", name = "진라면 매운맛 5입")
            val existing = ProductBarcode(customKey = "100100EA001", barcode = "old-barcode")
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns listOf(product)
            every { productBarcodeRepository.findByCustomKey("100100EA001") } returns existing
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(productBarcode = "new-barcode")))

            val saved = savedSlot.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.barcode).isEqualTo("new-barcode")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("Product 매칭 실패 - failures, 적재 스킵")
        fun upsert_productNotFound() {
            every { productRepository.findByProductCodeIn(listOf("999999")) } returns emptyList()
            every { productBarcodeRepository.findByCustomKey(any()) } returns null

            val result = service.upsert(listOf(command(productCode = "999999")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("999999EA001")
            assertThat(result.failures.single().reason).contains("product_code not found")
            verify(exactly = 0) { productBarcodeRepository.saveAll(any<List<ProductBarcode>>()) }
        }

        @Test
        @DisplayName("ProductCode 누락 - failures (identifier null)")
        fun upsert_missingProductCode() {
            val result = service.upsert(listOf(command(productCode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("ProductCode 필수")
        }

        @Test
        @DisplayName("ProductUnit 누락 - failures")
        fun upsert_missingProductUnit() {
            every { productRepository.findByProductCodeIn(any()) } returns emptyList()

            val result = service.upsert(listOf(command(productUnit = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("ProductUnit 필수")
        }

        @Test
        @DisplayName("ProductBarcode 누락 - failures (identifier=customKey)")
        fun upsert_missingBarcode() {
            every { productRepository.findByProductCodeIn(any()) } returns emptyList()
            every { productBarcodeRepository.findByCustomKey(any()) } returns null

            val result = service.upsert(listOf(command(productBarcode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100100EA001")
            assertThat(result.failures.single().reason).contains("ProductBarcode 필수")
        }
    }
}
