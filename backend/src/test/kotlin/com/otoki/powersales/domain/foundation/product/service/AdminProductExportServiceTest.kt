package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.domain.foundation.product.service.AdminProductExportService
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.io.ByteArrayInputStream

@DisplayName("AdminProductExportService 테스트")
class AdminProductExportServiceTest {

    private val productRepository: ProductRepository = mockk()

    private val service = AdminProductExportService(
        productRepository,
    )

    @Test
    @DisplayName("정상 케이스 — 조회 조건 결과의 .xlsx 바이트 생성, 헤더 11개 + 데이터 행 + tasteGift 변환")
    fun exportByCondition_success() {
        stubSearch(
            createProduct(productCode = "P001", name = "꿀배청 680G", tasteGift = "1"),
            createProduct(productCode = "P002", name = "카레 100G", tasteGift = "2")
        )

        val bytes = service.exportByCondition(
            keyword = null, category1 = null, category2 = null, category3 = null, productStatus = null
        )

        assertThat(bytes).isNotEmpty
        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.sheetName).isEqualTo("제품")
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
    @DisplayName("조회 조건은 목록 조회와 동일하게 searchForAdmin 에 그대로 전달 — 상한 단일 페이지")
    fun exportByCondition_passesFiltersThrough() {
        stubSearch()
        val pageable = slot<Pageable>()

        service.exportByCondition(
            keyword = "라면",
            category1 = "면류",
            category2 = "봉지면",
            category3 = "매운맛",
            productStatus = "판매중"
        )

        verify {
            productRepository.searchForAdmin(
                keyword = "라면",
                category1 = "면류",
                category2 = "봉지면",
                category3 = "매운맛",
                productStatus = "판매중",
                pageable = capture(pageable)
            )
        }
        assertThat(pageable.captured.pageNumber).isEqualTo(0)
        assertThat(pageable.captured.pageSize).isEqualTo(AdminProductExportService.EXPORT_MAX_ROWS)
    }

    @Test
    @DisplayName("tasteGift 알 수 없는 코드 — 원본 그대로 출력")
    fun exportByCondition_unknownTasteGift() {
        stubSearch(createProduct(productCode = "P003", name = "기타", tasteGift = "X"))

        val bytes = service.exportByCondition(
            keyword = null, category1 = null, category2 = null, category3 = null, productStatus = null
        )

        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.getRow(1).getCell(10).stringCellValue).isEqualTo("X")
        }
    }

    @Test
    @DisplayName("조회 결과 0건 — 빈 시트 (헤더만)")
    fun exportByCondition_empty() {
        stubSearch()

        val bytes = service.exportByCondition(
            keyword = null, category1 = null, category2 = null, category3 = null, productStatus = null
        )

        WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertThat(sheet.lastRowNum).isEqualTo(0)
        }
    }

    private fun stubSearch(vararg products: Product) {
        every {
            productRepository.searchForAdmin(any(), any(), any(), any(), any(), any())
        } returns PageImpl(products.toList())
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
