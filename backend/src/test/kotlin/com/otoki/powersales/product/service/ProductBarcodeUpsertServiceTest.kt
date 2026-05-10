package com.otoki.powersales.product.service

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.entity.ProductBarcode
import com.otoki.powersales.product.repository.ProductBarcodeRepository
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.product.service.dto.ProductBarcodeUpsertCommand
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductBarcodeUpsertService 테스트")
class ProductBarcodeUpsertServiceTest {

    @Mock
    private lateinit var productBarcodeRepository: ProductBarcodeRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var service: ProductBarcodeUpsertService

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

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, FK 매핑, name=ProductUnit")
        fun upsert_insertNew() {
            val product = Product(id = 7L, productCode = "100100", name = "진라면 매운맛 5입")
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(listOf(product))
            whenever(productBarcodeRepository.findByCustomKey("100100EA001")).thenReturn(null)

            val result = service.upsert(listOf(command()))

            val captor = argumentCaptor<List<ProductBarcode>>()
            verify(productBarcodeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
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
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(listOf(product))
            whenever(productBarcodeRepository.findByCustomKey("100100EA001")).thenReturn(existing)

            service.upsert(listOf(command(productBarcode = "new-barcode")))

            val captor = argumentCaptor<List<ProductBarcode>>()
            verify(productBarcodeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
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
            whenever(productRepository.findByProductCodeIn(listOf("999999"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(productCode = "999999")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("999999EA001")
            assertThat(result.failures.single().reason).contains("product_code not found")
            verify(productBarcodeRepository, never()).saveAll(any<List<ProductBarcode>>())
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
            val result = service.upsert(listOf(command(productUnit = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("ProductUnit 필수")
        }

        @Test
        @DisplayName("ProductBarcode 누락 - failures (identifier=customKey)")
        fun upsert_missingBarcode() {
            val result = service.upsert(listOf(command(productBarcode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100100EA001")
            assertThat(result.failures.single().reason).contains("ProductBarcode 필수")
        }
    }
}
