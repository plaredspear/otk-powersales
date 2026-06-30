package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.repository.ProductBarcodeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertCommand
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException

@DisplayName("ProductBarcodeUpsertService 테스트")
class ProductBarcodeUpsertServiceTest {

    private val productBarcodeRepository: ProductBarcodeRepository = mockk()
    private val productRepository: ProductRepository = mockk()

    // 행 단위 트랜잭션 빈 — 테스트에서는 실제 빈을 mock repository 로 구성 (REQUIRES_NEW 는 단위 테스트 무관).
    private val rowUpsertService = ProductBarcodeRowUpsertService(productBarcodeRepository)

    private val service = ProductBarcodeUpsertService(
        productRepository,
        rowUpsertService,
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

    private fun stubSaveAndFlushCapture(): CapturingSlot<ProductBarcode> {
        val slot = slot<ProductBarcode>()
        every { productBarcodeRepository.saveAndFlush(capture(slot)) } answers { firstArg<ProductBarcode>() }
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
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(command()))

            val saved = savedSlot.captured
            assertThat(saved.customKey).isEqualTo("100100EA001")
            assertThat(saved.name).isEqualTo("EA")
            assertThat(saved.unit).isEqualTo("EA")
            assertThat(saved.barcode).isEqualTo("8801045123456")
            assertThat(saved.sortOrder).isEqualTo("001")
            assertThat(saved.productId).isEqualTo(7L)
            assertThat(saved.productCode).isEqualTo("100100")
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("기존 customKey - UPDATE, barcode 변경")
        fun upsert_updateExisting() {
            val product = Product(id = 7L, productCode = "100100", name = "진라면 매운맛 5입")
            val existing = ProductBarcode(customKey = "100100EA001", barcode = "old-barcode")
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns listOf(product)
            every { productBarcodeRepository.findByCustomKey("100100EA001") } returns existing
            val savedSlot = stubSaveAndFlushCapture()

            service.upsert(listOf(command(productBarcode = "new-barcode")))

            val saved = savedSlot.captured
            assertThat(saved).isSameAs(existing)
            assertThat(saved.barcode).isEqualTo("new-barcode")
        }

        @Test
        @DisplayName("Product 매칭 실패 - orphan 저장 (productId=null, product_code 평문 보존, 레거시 정합)")
        fun upsert_productNotFound() {
            every { productRepository.findByProductCodeIn(listOf("999999")) } returns emptyList()
            every { productBarcodeRepository.findByCustomKey(any()) } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(command(productCode = "999999")))

            val saved = savedSlot.captured
            assertThat(saved.customKey).isEqualTo("999999EA001")
            assertThat(saved.productId).isNull()
            assertThat(saved.productCode).isEqualTo("999999")
            assertThat(saved.barcode).isEqualTo("8801045123456")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("upsert - 레거시 정합 (네 필드 필수 검증 제거 — nillable raw 적재)")
    inner class UpsertLegacyAlignment {

        @Test
        @DisplayName("ProductBarcode 누락 - 검증 없이 barcode=null 로 적재 (SF nillable=true 정합)")
        fun upsert_missingBarcode_rawStored() {
            every { productRepository.findByProductCodeIn(any()) } returns emptyList()
            every { productBarcodeRepository.findByCustomKey("100100EA001") } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(command(productBarcode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.customKey).isEqualTo("100100EA001")
            assertThat(savedSlot.captured.barcode).isNull()
        }

        @Test
        @DisplayName("ProductUnit/Sequence 누락 - 검증 없이 부분 키로 적재 (null 은 빈 문자열 연결)")
        fun upsert_missingUnitSequence_rawStored() {
            every { productRepository.findByProductCodeIn(any()) } returns emptyList()
            // customKey = "100100" + "" + "" = "100100"
            every { productBarcodeRepository.findByCustomKey("100100") } returns null
            val savedSlot = stubSaveAndFlushCapture()

            val result = service.upsert(listOf(command(productUnit = null, productSequence = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.customKey).isEqualTo("100100")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path (custom_key UNIQUE 충돌 행 격리)")
    inner class UpsertError {

        @Test
        @DisplayName("custom_key UNIQUE 충돌 - 그 행만 failures, 트랜잭션 전체 롤백 안 함")
        fun upsert_uniqueViolation_isolatedAsFailure() {
            every { productRepository.findByProductCodeIn(any()) } returns emptyList()
            every { productBarcodeRepository.findByCustomKey(any()) } returns null
            // 첫 행 정상, 둘째 행 UNIQUE 충돌
            every { productBarcodeRepository.saveAndFlush(match { it.customKey == "100100EA001" }) } answers { firstArg() }
            every { productBarcodeRepository.saveAndFlush(match { it.customKey == "200200EA001" }) } throws
                DataIntegrityViolationException("duplicate key value violates unique constraint")

            val result = service.upsert(
                listOf(
                    command(productCode = "100100"),
                    command(productCode = "200200")
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("200200EA001")
            assertThat(result.failures.single().reason).contains("적재 실패")
        }
    }
}
