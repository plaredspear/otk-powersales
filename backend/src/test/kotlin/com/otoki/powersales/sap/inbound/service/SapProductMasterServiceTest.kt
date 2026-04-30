package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterRequestItem
import com.otoki.powersales.product.repository.ProductRepository
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
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("SapProductMasterService 테스트")
class SapProductMasterServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapProductMasterService

    private fun item(
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
        productBarcode: String? = null
    ): ProductMasterRequestItem = ProductMasterRequestItem(
        productCode = productCode,
        productName = productName,
        standardPrice = standardPrice,
        boxReceivingQuantity = boxReceivingQuantity,
        superTax = superTax,
        launchDate = launchDate,
        productStatus = productStatus,
        unit = unit,
        category1 = category1,
        storeCondition = storeCondition,
        logisticsBarCode = logisticsBarCode,
        productBarcode = productBarcode
    )

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val detail = service.upsert(
                listOf(item(standardPrice = "4500", launchDate = "20200101"))
            )

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.productCode).isEqualTo("100100")
            assertThat(saved.name).isEqualTo("진라면 매운맛 5입")
            assertThat(saved.standardPrice).isEqualTo(4500.0)
            assertThat(saved.launchDate).isEqualTo(LocalDate.of(2020, 1, 1))
            assertThat(saved.superTax).isEqualTo(0.0)
            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
            verify(auditService).record(any<SapInboundAudit>())
        }

        @Test
        @DisplayName("기존 갱신 - 동일 productCode, mutable 필드만 갱신")
        fun upsert_updateExisting() {
            val existing = Product(productCode = "100100", name = "기존명")
            existing.standardPrice = 1000.0
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(listOf(existing))

            service.upsert(listOf(item(productName = "신규명", standardPrice = "5000")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("신규명")
            assertThat(saved.standardPrice).isEqualTo(5000.0)
        }

        @Test
        @DisplayName("LaunchDate 00000000 - launchDate=null")
        fun upsert_launchDateZero() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(launchDate = "00000000")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().launchDate).isNull()
            assertThat(detail.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("StoreCondition 매핑 - storageCondition 컬럼에 저장")
        fun upsert_storeConditionMapping() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            service.upsert(listOf(item(storeCondition = "냉장보관")))

            val captor = argumentCaptor<List<Product>>()
            verify(productRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().storageCondition).isEqualTo("냉장보관")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("ProductCode 누락 - failures, 적재 스킵")
        fun upsert_missingProductCode() {
            val detail = service.upsert(listOf(item(productCode = null)))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isNull()
            assertThat(detail.failures.single().reason).contains("ProductCode 필수")
            verify(productRepository, never()).saveAll(any<List<Product>>())
        }

        @Test
        @DisplayName("StandardPrice 변환 실패 - failures, 적재 스킵")
        fun upsert_invalidStandardPrice() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(standardPrice = "abc")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("100100")
            assertThat(detail.failures.single().reason).contains("StandardPrice 변환 실패")
        }

        @Test
        @DisplayName("ProductName 누락 - failures")
        fun upsert_missingProductName() {
            whenever(productRepository.findByProductCodeIn(listOf("100100"))).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(productName = null)))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("ProductName 필수")
        }
    }
}
