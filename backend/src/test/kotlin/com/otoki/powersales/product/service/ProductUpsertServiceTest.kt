package com.otoki.powersales.product.service

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.enums.StorageCondition
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.product.service.dto.ProductUpsertCommand
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
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("ProductUpsertService 테스트")
class ProductUpsertServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var service: ProductUpsertService

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

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(standardPrice = "4500", launchDate = "20200101")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.productCode).isEqualTo("100100")
            assertThat(saved.name).isEqualTo("진라면 매운맛 5입")
            assertThat(saved.standardUnitPrice).isEqualByComparingTo(java.math.BigDecimal("4500"))
            assertThat(saved.launchDate).isEqualTo(LocalDate.of(2020, 1, 1))
            assertThat(saved.superTax).isEqualByComparingTo(java.math.BigDecimal.ZERO)
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 갱신 - 동일 productCode, mutable 필드만 갱신")
        fun upsert_updateExisting() {
            val existing = Product(productCode = "100100", name = "기존명")
            existing.standardUnitPrice = java.math.BigDecimal("1000")
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(listOf(existing))

            service.upsert(listOf(command(productName = "신규명", standardPrice = "5000")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("신규명")
            assertThat(saved.standardUnitPrice).isEqualByComparingTo(java.math.BigDecimal("5000"))
        }

        @Test
        @DisplayName("LaunchDate 00000000 - launchDate=null")
        fun upsert_launchDateZero() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(launchDate = "00000000")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().launchDate).isNull()
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("StoreCondition 매핑 - storageCondition 컬럼에 enum 변환 저장")
        fun upsert_storeConditionMapping() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            service.upsert(listOf(command(storeCondition = "냉장")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().storageCondition)
                .isEqualTo(StorageCondition.REFRIGERATED)
        }

        @Test
        @DisplayName("Spec #575 - ProductBarcode + Pallet 정상 매핑")
        fun upsert_legacyFieldsMapped() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            service.upsert(listOf(command(productBarcode = "8801007123456", pallet = "100")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.productBarcode).isEqualTo("8801007123456")
            assertThat(saved.pallet).isEqualByComparingTo(BigDecimal("100"))
        }

        @Test
        @DisplayName("Spec #575 - Pallet blank → 0")
        fun upsert_palletBlank() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            service.upsert(listOf(command(pallet = "")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().pallet).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        @DisplayName("Spec #575 - Pallet null (필드 미포함) → 0")
        fun upsert_palletNull() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            service.upsert(listOf(command(pallet = null)))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().pallet).isEqualByComparingTo(BigDecimal.ZERO)
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
            verify(productRepository, never()).saveAll(any<List<Product>>())
        }

        @Test
        @DisplayName("StandardPrice 변환 실패 - failures, 적재 스킵")
        fun upsert_invalidStandardPrice() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(standardPrice = "abc")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100100")
            assertThat(result.failures.single().reason).contains("StandardPrice 변환 실패")
        }

        @Test
        @DisplayName("ProductName 누락 - failures")
        fun upsert_missingProductName() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(productName = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("ProductName 필수")
        }

        @Test
        @DisplayName("Spec #575 - Pallet 비숫자 → 행 단위 부분 실패")
        fun upsert_palletInvalid() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(pallet = "abc")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100100")
            assertThat(result.failures.single().reason).contains("Pallet 형식 오류: abc")
            verify(productRepository, never()).saveAll(any<List<Product>>())
        }

        @Test
        @DisplayName("LaunchDate 형식 오류 - failures")
        fun upsert_launchDateInvalid() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(launchDate = "2020-01-01")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("LaunchDate 형식 오류")
        }
    }
}
