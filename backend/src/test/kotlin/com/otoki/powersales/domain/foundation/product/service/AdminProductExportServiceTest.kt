package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.domain.foundation.product.service.AdminProductExportService
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

@DisplayName("AdminProductExportService 테스트")
class AdminProductExportServiceTest {

    private val productRepository: ProductRepository = mockk()

    private val service = AdminProductExportService(
        productRepository,
    )

    @Test
    @DisplayName("정상 케이스 — 선택 제품의 .xlsx 바이트 생성, 헤더 11개 + 데이터 행 + tasteGift 변환")
    fun exportSelectedProducts_success() {
        every { productRepository.findByProductCodeIn(listOf("P001", "P002")) } returns listOf(
            createProduct(productCode = "P001", name = "꿀배청 680G", tasteGift = "1"),
            createProduct(productCode = "P002", name = "카레 100G", tasteGift = "2")
        )

        val bytes = service.exportSelectedProducts(listOf("P001", "P002"))

        assertThat(bytes).isNotEmpty
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.sheetName).isEqualTo("선택제품")
            val header = sheet.getRow(0)
            assertThat(header.lastCellNum.toInt()).isEqualTo(11)
            assertThat(header.getCell(0).stringCellValue).isEqualTo("제품코드")
            assertThat(header.getCell(10).stringCellValue).isEqualTo("형태구분")
            val row1 = sheet.getRow(1)
            assertThat(row1.getCell(0).stringCellValue).isEqualTo("P001")
            assertThat(row1.getCell(10).stringCellValue).isEqualTo("전용")
            val row2 = sheet.getRow(2)
            assertThat(row2.getCell(0).stringCellValue).isEqualTo("P002")
            assertThat(row2.getCell(10).stringCellValue).isEqualTo("범용")
        }
    }

    @Test
    @DisplayName("tasteGift 알 수 없는 코드 — 원본 그대로 출력")
    fun exportSelectedProducts_unknownTasteGift() {
        every { productRepository.findByProductCodeIn(listOf("P003")) } returns listOf(
            createProduct(productCode = "P003", name = "기타", tasteGift = "X")
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
        every { productRepository.findByProductCodeIn(emptyList()) } returns emptyList()

        val bytes = service.exportSelectedProducts(emptyList())

        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.lastRowNum).isEqualTo(0)
        }
    }

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
