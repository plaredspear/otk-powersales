package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.domain.foundation.product.service.ProductUpsertService
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import com.otoki.powersales.domain.foundation.product.service.dto.ProductUpsertCommand
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("ProductUpsertService 테스트")
class ProductUpsertServiceTest {

    private val productRepository: ProductRepository = mockk()

    private val service = ProductUpsertService(
        productRepository,
    )

    private fun command(
        productCode: String? = "100100",
        productName: String? = "진라면 매운맛 5입",
        standardPrice: String? = null,
        boxReceivingQuantity: String? = null,
        superTax: String? = null,
        launchDate: String? = null,
        productStatus: String? = null,
        unit: String? = null,
        category1: String? = null,
        storeCondition: String? = null,
        logisticsBarCode: String? = null,
        productBarcode: String? = null,
        pallet: String? = null
    ): ProductUpsertCommand = ProductUpsertCommand(
        productCode = productCode,
        productName = productName,
        productBarcode = productBarcode,
        logisticsBarCode = logisticsBarCode,
        categoryCode1 = null,
        category1 = category1,
        categoryCode2 = null,
        category2 = null,
        categoryCode3 = null,
        category3 = null,
        productStatus = productStatus,
        standardPrice = standardPrice,
        unit = unit,
        boxReceivingQuantity = boxReceivingQuantity,
        shelfLife = null,
        shelfLifeUnit = null,
        launchDate = launchDate,
        storeCondition = storeCondition,
        productType = null,
        superTax = superTax,
        tasteGift = null,
        pallet = pallet
    )

    private fun stubSaveAllCapture(): CapturingSlot<List<Product>> {
        val slot = slot<List<Product>>()
        every { productRepository.saveAll(capture(slot)) } answers { firstArg<List<Product>>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(standardPrice = "4500", launchDate = "20200101")))

            val saved = savedSlot.captured.single()
            assertThat(saved.productCode).isEqualTo("100100")
            assertThat(saved.name).isEqualTo("진라면 매운맛 5입")
            assertThat(saved.standardUnitPrice).isEqualByComparingTo(BigDecimal("4500"))
            assertThat(saved.launchDate).isEqualTo(LocalDate.of(2020, 1, 1))
            assertThat(saved.superTax).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 갱신 - 동일 productCode, mutable 필드만 갱신")
        fun upsert_updateExisting() {
            val existing = Product(productCode = "100100", name = "기존명")
            existing.standardUnitPrice = BigDecimal("1000")
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns listOf(existing)
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(productName = "신규명", standardPrice = "5000")))

            val saved = savedSlot.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("신규명")
            assertThat(saved.standardUnitPrice).isEqualByComparingTo(BigDecimal("5000"))
        }

        @Test
        @DisplayName("LaunchDate 00000000 - launchDate=null")
        fun upsert_launchDateZero() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(launchDate = "00000000")))

            assertThat(savedSlot.captured.single().launchDate).isNull()
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("StoreCondition 매핑 - storageCondition 컬럼에 enum 변환 저장")
        fun upsert_storeConditionMapping() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(storeCondition = "냉장")))

            assertThat(savedSlot.captured.single().storageCondition).isEqualTo(StorageCondition.REFRIGERATED)
        }

        @Test
        @DisplayName("Spec #575 - ProductBarcode → barcode(DKRetail__Barcode__c) + Pallet 정상 매핑")
        fun upsert_legacyFieldsMapped() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(productBarcode = "8801007123456", pallet = "100")))

            val saved = savedSlot.captured.single()
            // 레거시 ProductBarcode → DKRetail__Barcode__c 적재 동등 (admin 제품 상세 barcode 노출 정합).
            assertThat(saved.barcode).isEqualTo("8801007123456")
            assertThat(saved.pallet).isEqualByComparingTo(BigDecimal("100"))
        }

        @Test
        @DisplayName("Spec #575 - Pallet blank → 0")
        fun upsert_palletBlank() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(pallet = "")))

            assertThat(savedSlot.captured.single().pallet).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        @DisplayName("Spec #575 - Pallet null (필드 미포함) → 0")
        fun upsert_palletNull() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(pallet = null)))

            assertThat(savedSlot.captured.single().pallet).isEqualByComparingTo(BigDecimal.ZERO)
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("ProductCode 누락 - failures, 적재 스킵, identifier null")
        fun upsert_missingProductCode() {
            val result = service.upsert(listOf(command(productCode = null)))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("ProductCode 필수")
            verify(exactly = 0) { productRepository.saveAll(any<List<Product>>()) }
        }

        @Test
        @DisplayName("StandardPrice 변환 실패 - failures, 적재 스킵")
        fun upsert_invalidStandardPrice() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            every { productRepository.saveAll(any<List<Product>>()) } answers { firstArg<List<Product>>() }

            val result = service.upsert(listOf(command(standardPrice = "abc")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100100")
            assertThat(result.failures.single().reason).contains("StandardPrice 변환 실패")
        }

        @Test
        @DisplayName("ProductName 누락 - failures")
        fun upsert_missingProductName() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            every { productRepository.saveAll(any<List<Product>>()) } answers { firstArg<List<Product>>() }

            val result = service.upsert(listOf(command(productName = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("ProductName 필수")
        }

        @Test
        @DisplayName("Spec #575 - Pallet 비숫자 → 행 단위 부분 실패")
        fun upsert_palletInvalid() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            every { productRepository.saveAll(any<List<Product>>()) } answers { firstArg<List<Product>>() }

            val result = service.upsert(listOf(command(pallet = "abc")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100100")
            assertThat(result.failures.single().reason).contains("Pallet 형식 오류: abc")
        }

        @Test
        @DisplayName("LaunchDate 형식 오류 - failures")
        fun upsert_launchDateInvalid() {
            every { productRepository.findByProductCodeIn(listOf("100100")) } returns emptyList()
            every { productRepository.saveAll(any<List<Product>>()) } answers { firstArg<List<Product>>() }

            val result = service.upsert(listOf(command(launchDate = "2020-01-01")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("LaunchDate 형식 오류")
        }
    }
}
