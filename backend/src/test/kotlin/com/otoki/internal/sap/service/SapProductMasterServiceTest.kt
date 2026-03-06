package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.repository.ProductRepository
import com.otoki.internal.sap.dto.SapProductMasterRequest.ReqItem
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
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("SapProductMasterService 테스트")
class SapProductMasterServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var sapProductMasterService: SapProductMasterService

    @Nested
    @DisplayName("sync - 신규 제품 등록")
    inner class NewProductTests {

        @Test
        @DisplayName("정상 등록 - DB에 없는 product_code -> Insert")
        fun sync_newProduct_creates() {
            val items = listOf(createReqItem(productCode = "12345678", productName = "오뚜기 진라면"))
            whenever(productRepository.findByProductCode("12345678")).thenReturn(null)
            whenever(productRepository.save(any<Product>())).thenAnswer { it.getArgument<Product>(0) }

            val result = sapProductMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            assertThat(captor.firstValue.productCode).isEqualTo("12345678")
            assertThat(captor.firstValue.name).isEqualTo("오뚜기 진라면")
        }

        @Test
        @DisplayName("전체 필드 매핑 - 모든 요청 필드가 Product에 반영됨")
        fun sync_newProduct_fullFieldMapping() {
            val items = listOf(createReqItem(
                productCode = "12345678",
                productName = "오뚜기 진라면 순한맛 120g",
                logisticsBarcode = "18801045520008",
                categoryCode1 = "10",
                category1 = "면류",
                categoryCode2 = "1010",
                category2 = "라면",
                categoryCode3 = "101010",
                category3 = "봉지라면",
                productStatus = "10",
                standardPrice = "850",
                unit = "EA",
                boxReceivingQuantity = "40",
                shelfLife = "6",
                shelfLifeUnit = "M",
                launchDate = "20100301",
                storeCondition = "실온",
                productType = "일반",
                superTax = "0",
                tasteGift = ""
            ))
            whenever(productRepository.findByProductCode("12345678")).thenReturn(null)
            whenever(productRepository.save(any<Product>())).thenAnswer { it.getArgument<Product>(0) }

            sapProductMasterService.sync(items)

            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.logisticsBarcode).isEqualTo("18801045520008")
            assertThat(saved.categoryCode1).isEqualTo("10")
            assertThat(saved.category1).isEqualTo("면류")
            assertThat(saved.standardPrice).isEqualTo(850.0)
            assertThat(saved.boxReceivingQuantity).isEqualTo(40.0)
            assertThat(saved.launchDate).isEqualTo(LocalDate.of(2010, 3, 1))
            assertThat(saved.storageCondition).isEqualTo("실온")
            assertThat(saved.productType).isEqualTo("일반")
            assertThat(saved.superTax).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("sync - 기존 제품 업데이트")
    inner class ExistingProductTests {

        @Test
        @DisplayName("기존 제품 업데이트 - 필드 변경")
        fun sync_existingProduct_updates() {
            val existing = Product(id = 1, productCode = "12345678")
            existing.name = "기존이름"
            val items = listOf(createReqItem(productCode = "12345678", productName = "변경이름", standardPrice = "1000"))
            whenever(productRepository.findByProductCode("12345678")).thenReturn(existing)
            whenever(productRepository.save(any<Product>())).thenAnswer { it.getArgument<Product>(0) }

            val result = sapProductMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existing.name).isEqualTo("변경이름")
            assertThat(existing.standardPrice).isEqualTo(1000.0)
        }
    }

    @Nested
    @DisplayName("sync - 숫자 변환")
    inner class NumberConversionTests {

        @Test
        @DisplayName("숫자 변환 성공 - standard_price=850.5 -> 850.5")
        fun sync_numberConversion_success() {
            val items = listOf(createReqItem(productCode = "12345678", productName = "테스트", standardPrice = "850.5"))
            whenever(productRepository.findByProductCode("12345678")).thenReturn(null)
            whenever(productRepository.save(any<Product>())).thenAnswer { it.getArgument<Product>(0) }

            sapProductMasterService.sync(items)

            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            assertThat(captor.firstValue.standardPrice).isEqualTo(850.5)
        }

        @Test
        @DisplayName("null 처리 - super_tax=null -> 0.0")
        fun sync_nullNumber_defaultsToZero() {
            val items = listOf(createReqItem(productCode = "12345678", productName = "테스트", superTax = null))
            whenever(productRepository.findByProductCode("12345678")).thenReturn(null)
            whenever(productRepository.save(any<Product>())).thenAnswer { it.getArgument<Product>(0) }

            sapProductMasterService.sync(items)

            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            assertThat(captor.firstValue.superTax).isEqualTo(0.0)
        }

        @Test
        @DisplayName("빈 문자열 처리 - standard_price=\"\" -> 0.0")
        fun sync_emptyNumber_defaultsToZero() {
            val items = listOf(createReqItem(productCode = "12345678", productName = "테스트", standardPrice = ""))
            whenever(productRepository.findByProductCode("12345678")).thenReturn(null)
            whenever(productRepository.save(any<Product>())).thenAnswer { it.getArgument<Product>(0) }

            sapProductMasterService.sync(items)

            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            assertThat(captor.firstValue.standardPrice).isEqualTo(0.0)
        }

        @Test
        @DisplayName("잘못된 숫자 - standard_price=\"abc\" -> 해당 레코드 실패")
        fun sync_invalidNumber_fails() {
            val items = listOf(createReqItem(productCode = "12345678", productName = "테스트", standardPrice = "abc"))
            whenever(productRepository.findByProductCode("12345678")).thenReturn(null)

            val result = sapProductMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.successCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("product_code 누락 - 해당 레코드 실패")
        fun sync_missingProductCode_fails() {
            val items = listOf(createReqItem(productCode = null, productName = "테스트"))

            val result = sapProductMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("product_code")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            val items = listOf(
                createReqItem(productCode = "0001", productName = "성공1"),
                createReqItem(productCode = null, productName = "실패"),
                createReqItem(productCode = "0003", productName = "성공2")
            )
            whenever(productRepository.findByProductCode("0001")).thenReturn(null)
            whenever(productRepository.findByProductCode("0003")).thenReturn(null)
            whenever(productRepository.save(any<Product>())).thenAnswer { it.getArgument<Product>(0) }

            val result = sapProductMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    private fun createReqItem(
        productCode: String? = null,
        productName: String? = null,
        logisticsBarcode: String? = null,
        categoryCode1: String? = null,
        category1: String? = null,
        categoryCode2: String? = null,
        category2: String? = null,
        categoryCode3: String? = null,
        category3: String? = null,
        productStatus: String? = null,
        standardPrice: String? = null,
        unit: String? = null,
        boxReceivingQuantity: String? = null,
        shelfLife: String? = null,
        shelfLifeUnit: String? = null,
        launchDate: String? = null,
        storeCondition: String? = null,
        productType: String? = null,
        superTax: String? = null,
        tasteGift: String? = null
    ) = ReqItem(
        productCode = productCode,
        productName = productName,
        logisticsBarcode = logisticsBarcode,
        categoryCode1 = categoryCode1,
        category1 = category1,
        categoryCode2 = categoryCode2,
        category2 = category2,
        categoryCode3 = categoryCode3,
        category3 = category3,
        productStatus = productStatus,
        standardPrice = standardPrice,
        unit = unit,
        boxReceivingQuantity = boxReceivingQuantity,
        shelfLife = shelfLife,
        shelfLifeUnit = shelfLifeUnit,
        launchDate = launchDate,
        storeCondition = storeCondition,
        productType = productType,
        superTax = superTax,
        tasteGift = tasteGift
    )
}
