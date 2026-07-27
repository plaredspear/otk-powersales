package com.otoki.powersales.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.otoki.powersales.domain.foundation.product.dto.response.ProductListResponse
import com.otoki.powersales.domain.foundation.product.dto.response.ProductListItem
import com.otoki.powersales.domain.foundation.product.dto.response.CategoryTree
import com.otoki.powersales.domain.foundation.product.dto.response.Category2Node
import com.otoki.powersales.domain.foundation.product.dto.response.ProductDetail
import com.otoki.powersales.domain.foundation.product.dto.response.InventorySearchResponse
import com.otoki.powersales.domain.foundation.product.dto.response.InventorySearchResultItem
import com.otoki.powersales.domain.foundation.product.dto.request.InventorySearchRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import java.time.LocalDate
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.foundation.product.service.AdminProductExportService
import com.otoki.powersales.domain.foundation.product.service.AdminProductInventoryService
import com.otoki.powersales.domain.foundation.product.service.AdminProductService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.temporal.ChronoUnit

@WebMvcTest(AdminProductController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminProductController 테스트")
class AdminProductControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminProductService: AdminProductService
    @MockkBean private lateinit var adminProductInventoryService: AdminProductInventoryService
    @MockkBean private lateinit var adminProductExportService: AdminProductExportService

    @Nested
    @DisplayName("GET /api/v1/admin/products - 제품 목록 조회")
    inner class GetProducts {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getProducts_success() {
            val response = ProductListResponse(
                content = listOf(
                    ProductListItem(
                        id = 1L,
                        productCode = "P001",
                        name = "진라면 매운맛",
                        category1 = "면류",
                        category2 = "라면",
                        category3 = "봉지면",
                        standardUnitPrice = BigDecimal("850.00"),
                        unit = "EA",
                        storageCondition = "실온",
                        productStatus = "판매중",
                        launchDate = "2020-01-15",
                        superTax = BigDecimal("85"),
                        shelfLife = "12",
                        shelfLifeUnit = "월",
                        tasteGift = "1",
                        lastModifiedAt = "2026-01-01T00:00:00"
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            every { adminProductService.getProducts(
                keyword = null, category1 = null, category2 = null,
                category3 = null, productStatus = null,
                page = eq(0), size = eq(20)
            ) } returns response

            mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }

        @Test
        @DisplayName("성공 - 키워드 + 카테고리 필터")
        fun getProducts_withFilters_success() {
            val response = ProductListResponse(
                content = emptyList(),
                page = 0, size = 10, totalElements = 0, totalPages = 0
            )
            every { adminProductService.getProducts(
                keyword = eq("진라면"), category1 = eq("면류"), category2 = null,
                category3 = null, productStatus = null,
                page = eq(0), size = eq(10)
            ) } returns response

            mockMvc.perform(
                get("/api/v1/admin/products")
                    .param("keyword", "진라면")
                    .param("category1", "면류")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty)
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getProducts_empty_success() {
            val response = ProductListResponse(
                content = emptyList(),
                page = 0, size = 20, totalElements = 0, totalPages = 0
            )
            every { adminProductService.getProducts(
                keyword = null, category1 = null, category2 = null,
                category3 = null, productStatus = null,
                page = eq(0), size = eq(20)
            ) } returns response

            mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products/categories - 카테고리 목록 조회")
    inner class GetCategories {

        @Test
        @DisplayName("성공 - 카테고리 트리 반환")
        fun getCategories_success() {
            val response = listOf(
                CategoryTree(
                    category1 = "면류",
                    children = listOf(
                        Category2Node(category2 = "라면", children = listOf("봉지면", "컵라면"))
                    )
                )
            )
            every { adminProductService.getCategories() } returns response

            mockMvc.perform(get("/api/v1/admin/products/categories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].category1").value("면류"))
                .andExpect(jsonPath("$.data[0].children[0].category2").value("라면"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products/lookup - 행사마스터 제품 lookup search")
    inner class LookupProducts {

        @Test
        @DisplayName("성공 - 카테고리 미지정 시 분류 필터 없이 keyword 만 전달 (드롭다운 빠른 검색)")
        fun lookupProducts_keywordOnly_success() {
            val response = ProductListResponse(
                content = emptyList(),
                page = 0, size = 20, totalElements = 45, totalPages = 3
            )
            every { adminProductService.getProducts(
                keyword = eq("진라면_매운맛"), category1 = null, category2 = null,
                category3 = null, productStatus = null,
                page = eq(0), size = eq(20)
            ) } returns response

            mockMvc.perform(
                get("/api/v1/admin/products/lookup")
                    .param("keyword", "진라면_매운맛")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalElements").value(45))
        }

        @Test
        @DisplayName("성공 - 고급 검색 모달의 대/중/소분류 필터가 서비스로 전달된다")
        fun lookupProducts_withCategoryFilters_success() {
            val response = ProductListResponse(
                content = emptyList(),
                page = 0, size = 20, totalElements = 0, totalPages = 0
            )
            every { adminProductService.getProducts(
                keyword = eq("진라면"), category1 = eq("라면류"), category2 = eq("봉지면류"),
                category3 = eq("가정"), productStatus = null,
                page = eq(0), size = eq(20)
            ) } returns response

            mockMvc.perform(
                get("/api/v1/admin/products/lookup")
                    .param("keyword", "진라면")
                    .param("category1", "라면류")
                    .param("category2", "봉지면류")
                    .param("category3", "가정")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("성공 - 고급 검색 모달의 제품상태 필터가 서비스로 전달된다")
        fun lookupProducts_withProductStatus_success() {
            val response = ProductListResponse(
                content = emptyList(),
                page = 0, size = 20, totalElements = 0, totalPages = 0
            )
            every { adminProductService.getProducts(
                keyword = null, category1 = null, category2 = null,
                category3 = null, productStatus = eq("단종"),
                page = eq(0), size = eq(20)
            ) } returns response

            mockMvc.perform(
                get("/api/v1/admin/products/lookup")
                    .param("productStatus", "단종")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("성공 - 고급 검색 모달의 페이지 이동으로 2페이지 이후 결과에 도달한다")
        fun lookupProducts_pagination_success() {
            val response = ProductListResponse(
                content = emptyList(),
                page = 2, size = 20, totalElements = 45, totalPages = 3
            )
            every { adminProductService.getProducts(
                keyword = eq("진라면_매운맛"), category1 = null, category2 = null,
                category3 = null, productStatus = null,
                page = eq(2), size = eq(20)
            ) } returns response

            mockMvc.perform(
                get("/api/v1/admin/products/lookup")
                    .param("keyword", "진라면_매운맛")
                    .param("page", "2")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.page").value(2))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products/lookup-filter-options - 고급 검색 필터 옵션")
    inner class LookupFilterOptions {

        @Test
        @DisplayName("성공 - 카테고리 트리 반환 (product.READ 불요, promotion.READ 가드)")
        fun lookupFilterOptions_success() {
            val categories = listOf(
                CategoryTree(
                    category1 = "라면류",
                    children = listOf(
                        Category2Node(category2 = "봉지면류", children = listOf("가정", "업소", "수출"))
                    )
                )
            )
            every { adminProductService.getCategories() } returns categories

            mockMvc.perform(get("/api/v1/admin/products/lookup-filter-options"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.categories[0].category1").value("라면류"))
                .andExpect(jsonPath("$.data.categories[0].children[0].category2").value("봉지면류"))
                .andExpect(jsonPath("$.data.categories[0].children[0].children[0]").value("가정"))
                // 제품상태 선택지는 저장값이 아니라 화면 표시명 — "판매중"(값 없음) / "단종"(출고중지).
                .andExpect(jsonPath("$.data.productStatuses").isArray)
                .andExpect(jsonPath("$.data.productStatuses[0]").value("판매중"))
                .andExpect(jsonPath("$.data.productStatuses[1]").value("단종"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products/{productCode} - 제품 상세 조회 (UC-02)")
    inner class GetProductDetail {

        @Test
        @DisplayName("성공 - 단건 상세 응답")
        fun getProductDetail_success() {
            val detail = ProductDetail(
                id = 1L,
                productCode = "P001",
                name = "꿀배청 680G",
                barcode = null, logisticsBarcode = null,
                category1 = "음료", category2 = "건강", category3 = "전통차",
                categoryCode1 = null, categoryCode2 = null, categoryCode3 = null,
                unit = "EA", orderingUnit = null,
                conversionQuantity = null, boxReceivingQuantity = null,
                standardUnitPrice = BigDecimal("5000"),
                superTax = null, launchDate = "2020-01-15",
                storageCondition = "실온", productStatus = "-", productType = null,
                shelfLife = "12", shelfLifeUnit = "월",
                tasteGift = null, productFeatures = null, sellingPoint = null,
                purpose = null, targetAccountType = null,
                allergen = null, crossContamination = null,
                imgRefPathFront = null, imgRefPathBack = null,
                pallet = null, manufacture = null, manufactureDetail = null,
                claimManagement = null,
                createdAt = "2026-01-01T00:00:00",
                lastModifiedAt = "2026-05-01T00:00:00",
                barcodes = emptyList()
            )
            every { adminProductService.getProductDetail(eq("P001")) } returns detail

            mockMvc.perform(get("/api/v1/admin/products/P001"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.productCode").value("P001"))
                .andExpect(jsonPath("$.data.name").value("꿀배청 680G"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/products/inventory-search - 재고조회 (UC-03/04)")
    inner class SearchInventory {

        private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

        @Test
        @DisplayName("성공 - SAP 응답 매핑")
        fun searchInventory_success() {
            val request = InventorySearchRequest(
                accountId = 1,
                productCodes = listOf("P001", "P002"),
                deliveryRequestDate = LocalDate.now().plus(1, ChronoUnit.DAYS)
            )
            val response = InventorySearchResponse(
                results = listOf(
                    InventorySearchResultItem(
                        productCode = "P001", productName = "꿀배청", unit = "EA",
                        conversionQuantity = 1, supplyLimitQuantity = 100,
                        unitPrice = BigDecimal("5000"), message = null
                    ),
                    InventorySearchResultItem(
                        productCode = "P002", productName = "카레", unit = "EA",
                        conversionQuantity = 1, supplyLimitQuantity = 200,
                        unitPrice = BigDecimal("3000"), message = null
                    )
                )
            )
            every { adminProductInventoryService.searchInventory(any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/products/inventory-search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.results.length()").value(2))
                .andExpect(jsonPath("$.data.results[0].productCode").value("P001"))
        }

        @Test
        @DisplayName("실패 - 제품 0건 -> 400 (validation)")
        fun searchInventory_emptyProducts() {
            val body = """{"accountId":1,"productCodes":[],"deliveryRequestDate":"${LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)}"}"""
            mockMvc.perform(
                post("/api/v1/admin/products/inventory-search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 제품 51건 -> 400 (validation)")
        fun searchInventory_tooManyProducts() {
            val codes = (1..51).map { "\"P$it\"" }.joinToString(",")
            val body = """{"accountId":1,"productCodes":[$codes],"deliveryRequestDate":"${LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)}"}"""
            mockMvc.perform(
                post("/api/v1/admin/products/inventory-search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products/export-excel - 엑셀 다운로드 (UC-05)")
    inner class ExportExcel {

        @Test
        @DisplayName("성공 - 조회 조건 무지정 시 전체 대상 + Content-Disposition")
        fun exportExcel_success() {
            // controller 후처리: Content-Disposition 헤더 + body bytes 매핑 (가드레일 5.3) — verbatim 유지
            val xlsxBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
            every {
                adminProductExportService.exportByCondition(
                    keyword = null, category1 = null, category2 = null,
                    category3 = null, productStatus = null
                )
            } returns xlsxBytes

            mockMvc.perform(get("/api/v1/admin/products/export-excel"))
                .andExpect(status().isOk)
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(xlsxBytes))
        }

        @Test
        @DisplayName("성공 - 조회 조건이 서비스로 그대로 전달")
        fun exportExcel_passesFilters() {
            val xlsxBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
            every {
                adminProductExportService.exportByCondition(
                    keyword = "라면", category1 = "면류", category2 = "봉지면",
                    category3 = "매운맛", productStatus = "판매중"
                )
            } returns xlsxBytes

            mockMvc.perform(
                get("/api/v1/admin/products/export-excel")
                    .param("keyword", "라면")
                    .param("category1", "면류")
                    .param("category2", "봉지면")
                    .param("category3", "매운맛")
                    .param("productStatus", "판매중")
            )
                .andExpect(status().isOk)
                .andExpect(content().bytes(xlsxBytes))
        }
    }
}
