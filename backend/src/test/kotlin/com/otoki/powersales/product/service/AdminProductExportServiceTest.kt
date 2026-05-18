package com.otoki.powersales.product.service

import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.enums.ProductStatus
import com.otoki.powersales.product.enums.StorageCondition
import com.otoki.powersales.product.repository.ProductRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminProductExportService 테스트")
class AdminProductExportServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var service: AdminProductExportService

    @Test
    @DisplayName("정상 케이스 — 선택 제품의 .xlsx 바이트 생성, 헤더 11개 + 데이터 행 + tasteGift 변환")
    fun exportSelectedProducts_success() {
        // Given
        whenever(productRepository.findByProductCodeIn(eq(listOf("P001", "P002")))).thenReturn(
            listOf(
                createProduct(productCode = "P001", name = "꿀배청 680G", tasteGift = "1"),
                createProduct(productCode = "P002", name = "카레 100G", tasteGift = "2")
            )
        )

        // When
        val bytes = service.exportSelectedProducts(listOf("P001", "P002"))

        // Then
        assertThat(bytes).isNotEmpty
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.sheetName).isEqualTo("선택제품")
            // 헤더 행
            val header = sheet.getRow(0)
            assertThat(header.lastCellNum.toInt()).isEqualTo(11)
            assertThat(header.getCell(0).stringCellValue).isEqualTo("제품코드")
            assertThat(header.getCell(10).stringCellValue).isEqualTo("형태구분")
            // 데이터 행 — 형태구분 코드 변환 검증
            val row1 = sheet.getRow(1)
            assertThat(row1.getCell(0).stringCellValue).isEqualTo("P001")
            assertThat(row1.getCell(10).stringCellValue).isEqualTo("전용") // tasteGift 1 -> 전용
            val row2 = sheet.getRow(2)
            assertThat(row2.getCell(0).stringCellValue).isEqualTo("P002")
            assertThat(row2.getCell(10).stringCellValue).isEqualTo("범용") // tasteGift 2 -> 범용
        }
    }

    @Test
    @DisplayName("tasteGift 알 수 없는 코드 — 원본 그대로 출력")
    fun exportSelectedProducts_unknownTasteGift() {
        whenever(productRepository.findByProductCodeIn(eq(listOf("P003")))).thenReturn(
            listOf(createProduct(productCode = "P003", name = "기타", tasteGift = "X"))
        )

        val bytes = service.exportSelectedProducts(listOf("P003"))

        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.getRow(1).getCell(10).stringCellValue).isEqualTo("X")
        }
    }

    @Test
    @DisplayName("빈 productCodes — 빈 시트 (헤더만)")
    fun exportSelectedProducts_empty() {
        whenever(productRepository.findByProductCodeIn(eq(emptyList()))).thenReturn(emptyList())

        val bytes = service.exportSelectedProducts(emptyList())

        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.lastRowNum).isEqualTo(0) // 헤더 행만
        }
    }

    // 출고중지 제외 분기 — ProductStatus enum 에 "출고중지" 값이 정의되면 별도 테스트 추가 권고.
    // 현재 enum 은 placeholder("-") 단일이라 분기 단언 작성 불가.

    private fun createProduct(
        productCode: String,
        name: String? = null,
        tasteGift: String? = null
    ): Product = Product(
        productCode = productCode,
        name = name,
        tasteGift = tasteGift,
        storageCondition = StorageCondition.fromDisplayNameOrNull("실온"),
        productStatus = ProductStatus.fromDisplayNameOrNull("-")
    )
}
