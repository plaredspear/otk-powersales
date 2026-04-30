package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.entity.Product
import com.otoki.powersales.sap.entity.ProductBarcode
import com.otoki.powersales.sap.inbound.dto.product.BarcodeMasterRequestItem
import com.otoki.powersales.sap.repository.ProductBarcodeRepository
import com.otoki.powersales.sap.repository.ProductRepository
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
@DisplayName("SapBarcodeMasterService 테스트")
class SapBarcodeMasterServiceTest {

    @Mock
    private lateinit var productBarcodeRepository: ProductBarcodeRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapBarcodeMasterService

    private fun item(
        productCode: String? = "100100",
        productName: String? = "진라면 매운맛 5입",
        productUnit: String? = "EA",
        productSequence: String? = "001",
        productBarcode: String? = "8801045123456"
    ): BarcodeMasterRequestItem = BarcodeMasterRequestItem(
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

            val detail = service.upsert(listOf(item()))

            val captor = argumentCaptor<List<ProductBarcode>>()
            verify(productBarcodeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.customKey).isEqualTo("100100EA001")
            assertThat(saved.name).isEqualTo("EA")
            assertThat(saved.unit).isEqualTo("EA")
            assertThat(saved.barcode).isEqualTo("8801045123456")
            assertThat(saved.sortOrder).isEqualTo("001")
            assertThat(saved.productId).isEqualTo(7L)
            assertThat(detail.successCount).isEqualTo(1)
            verify(auditService).record(any<SapInboundAudit>())
        }

        @Test
        @DisplayName("기존 customKey - UPDATE, barcode 변경")
        fun upsert_updateExisting() {
            val product = Product(id = 7L, productCode = "100100", name = "진라면 매운맛 5입")
            val existing = ProductBarcode(customKey = "100100EA001", barcode = "old-barcode")
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(listOf(product))
            whenever(productBarcodeRepository.findByCustomKey("100100EA001")).thenReturn(existing)

            service.upsert(listOf(item(productBarcode = "new-barcode")))

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

            val detail = service.upsert(listOf(item(productCode = "999999")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("product_code not found")
            verify(productBarcodeRepository, never()).saveAll(any<List<ProductBarcode>>())
        }

        @Test
        @DisplayName("ProductCode 누락 - failures")
        fun upsert_missingProductCode() {
            val detail = service.upsert(listOf(item(productCode = null)))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("ProductCode 필수")
        }
    }
}
