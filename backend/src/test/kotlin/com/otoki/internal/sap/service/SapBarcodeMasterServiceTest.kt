package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.ProductBarcode
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.repository.ProductRepository
import com.otoki.internal.sap.repository.ProductBarcodeRepository
import com.otoki.internal.sap.dto.SapBarcodeMasterRequest.ReqItem
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapBarcodeMasterService 테스트")
class SapBarcodeMasterServiceTest {

    @Mock
    private lateinit var productBarcodeRepository: ProductBarcodeRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var sapBarcodeMasterService: SapBarcodeMasterService

    @Nested
    @DisplayName("sync - 신규 바코드 등록")
    inner class NewBarcodeTests {

        @Test
        @DisplayName("정상 등록 - Product 매칭 성공")
        fun sync_newBarcode_withProduct() {
            val product = Product(id = 1, sfid = "a00000000000001", productCode = "12345678")
            val items = listOf(createReqItem(
                productCode = "12345678", productUnit = "EA", productSequence = "001",
                productBarcode = "8801045520001", productName = "오뚜기 진라면"
            ))
            whenever(productBarcodeRepository.findByCustomKey("12345678EA001")).thenReturn(null)
            whenever(productRepository.findByProductCode("12345678")).thenReturn(product)
            whenever(productBarcodeRepository.save(any<ProductBarcode>())).thenAnswer { it.getArgument<ProductBarcode>(0) }

            val result = sapBarcodeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            val captor = argumentCaptor<ProductBarcode>()
            verify(productBarcodeRepository).save(captor.capture())
            assertThat(captor.firstValue.customKey).isEqualTo("12345678EA001")
            assertThat(captor.firstValue.product).isEqualTo("a00000000000001")
            assertThat(captor.firstValue.productBarcode).isEqualTo("8801045520001")
        }

        @Test
        @DisplayName("Product 미매칭 - product__c=null, 에러 아님")
        fun sync_newBarcode_noProduct() {
            val items = listOf(createReqItem(
                productCode = "99999999", productUnit = "EA", productSequence = "001"
            ))
            whenever(productBarcodeRepository.findByCustomKey("99999999EA001")).thenReturn(null)
            whenever(productRepository.findByProductCode("99999999")).thenReturn(null)
            whenever(productBarcodeRepository.save(any<ProductBarcode>())).thenAnswer { it.getArgument<ProductBarcode>(0) }

            val result = sapBarcodeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            val captor = argumentCaptor<ProductBarcode>()
            verify(productBarcodeRepository).save(captor.capture())
            assertThat(captor.firstValue.product).isNull()
        }
    }

    @Nested
    @DisplayName("sync - 기존 바코드 업데이트")
    inner class ExistingBarcodeTests {

        @Test
        @DisplayName("기존 바코드 업데이트 - 필드 변경")
        fun sync_existingBarcode_updates() {
            val existing = ProductBarcode(id = 1, customKey = "12345678EA001")
            val items = listOf(createReqItem(
                productCode = "12345678", productUnit = "EA", productSequence = "001",
                productBarcode = "8801045520002", productName = "변경이름"
            ))
            whenever(productBarcodeRepository.findByCustomKey("12345678EA001")).thenReturn(existing)
            whenever(productRepository.findByProductCode("12345678")).thenReturn(null)
            whenever(productBarcodeRepository.save(any<ProductBarcode>())).thenAnswer { it.getArgument<ProductBarcode>(0) }

            val result = sapBarcodeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existing.productBarcode).isEqualTo("8801045520002")
            assertThat(existing.productName).isEqualTo("변경이름")
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("product_code 누락 - 해당 레코드 실패")
        fun sync_missingProductCode_fails() {
            val items = listOf(createReqItem(productCode = null, productUnit = "EA", productSequence = "001"))

            val result = sapBarcodeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("product_code")
        }

        @Test
        @DisplayName("product_unit 누락 - 해당 레코드 실패")
        fun sync_missingProductUnit_fails() {
            val items = listOf(createReqItem(productCode = "12345678", productUnit = null, productSequence = "001"))

            val result = sapBarcodeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("product_unit")
        }

        @Test
        @DisplayName("product_sequence 누락 - 해당 레코드 실패")
        fun sync_missingProductSequence_fails() {
            val items = listOf(createReqItem(productCode = "12345678", productUnit = "EA", productSequence = null))

            val result = sapBarcodeMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("product_sequence")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            val items = listOf(
                createReqItem(productCode = "0001", productUnit = "EA", productSequence = "001"),
                createReqItem(productCode = null, productUnit = "EA", productSequence = "001"),
                createReqItem(productCode = "0003", productUnit = "EA", productSequence = "001")
            )
            whenever(productBarcodeRepository.findByCustomKey("0001EA001")).thenReturn(null)
            whenever(productBarcodeRepository.findByCustomKey("0003EA001")).thenReturn(null)
            whenever(productRepository.findByProductCode("0001")).thenReturn(null)
            whenever(productRepository.findByProductCode("0003")).thenReturn(null)
            whenever(productBarcodeRepository.save(any<ProductBarcode>())).thenAnswer { it.getArgument<ProductBarcode>(0) }

            val result = sapBarcodeMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    private fun createReqItem(
        productCode: String? = null,
        productName: String? = null,
        productUnit: String? = null,
        productSequence: String? = null,
        productBarcode: String? = null
    ) = ReqItem(
        productCode = productCode,
        productName = productName,
        productUnit = productUnit,
        productSequence = productSequence,
        productBarcode = productBarcode
    )
}
