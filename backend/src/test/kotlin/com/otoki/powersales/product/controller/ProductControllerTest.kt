package com.otoki.powersales.product.controller

import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.product.dto.response.ProductDto
import com.otoki.powersales.product.exception.InvalidSearchParameterException
import com.otoki.powersales.product.exception.InvalidSearchTypeException
import com.otoki.powersales.product.service.ProductService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductController 테스트")
class ProductControllerTest : MobileControllerTestSupport() {

    @MockkBean private lateinit var productService: ProductService

    @Nested
    @DisplayName("검색 성공 케이스")
    inner class SuccessCases {

        @Test
        @DisplayName("텍스트 검색 성공 - 200 OK, 제품 목록 반환")
        fun searchProducts_textSearch_returnsOk() {
            val products = listOf(
                createProductDto("18110014", "열라면_용기105G", "18110014", "8801045570716"),
                createProductDto("18110007", "열라면_용기115G", "18110007", "8801045570723")
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 2)
            every { productService.searchProducts("열라면", "text", 0, 20) } returns page

            mockMvc.perform(
                get("/api/v1/mobile/products/search")
                    .param("query", "열라면")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].productCode").value("18110014"))
                .andExpect(jsonPath("$.data.totalElements").value(2))
        }

        @Test
        @DisplayName("바코드 검색 성공 - 200 OK")
        fun searchProducts_barcodeSearch_returnsOk() {
            val products = listOf(
                createProductDto("18110014", "열라면_용기105G", "18110014", "8801045570716")
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 1)
            every { productService.searchProducts("8801045570716", "barcode", 0, 20) } returns page

            mockMvc.perform(
                get("/api/v1/mobile/products/search")
                    .param("query", "8801045570716")
                    .param("type", "barcode")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content[0].logisticsBarcode").value("8801045570716"))
        }

        @Test
        @DisplayName("빈 결과 - 200 OK, 빈 목록")
        fun searchProducts_noResults_returnsEmptyList() {
            val page = PageImpl(emptyList<ProductDto>(), PageRequest.of(0, 20), 0)
            every { productService.searchProducts("존재하지않는제품", "text", 0, 20) } returns page

            mockMvc.perform(
                get("/api/v1/mobile/products/search")
                    .param("query", "존재하지않는제품")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }

        @Test
        @DisplayName("페이지네이션 파라미터 전달 성공")
        fun searchProducts_withPagination_passesParams() {
            val products = listOf(
                createProductDto("18130001", "참깨라면_봉지115G", "18130001", "8801045572001")
            )
            val page = PageImpl(products, PageRequest.of(1, 5), 6)
            every { productService.searchProducts("라면", "text", 1, 5) } returns page

            mockMvc.perform(
                get("/api/v1/mobile/products/search")
                    .param("query", "라면")
                    .param("page", "1")
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(2))
        }
    }

    @Nested
    @DisplayName("검색 에러 케이스")
    inner class ErrorCases {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.product.controller.ProductControllerTest#searchExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun searchProducts_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            query: String,
            type: String,
            exception: Throwable,
            expectedCode: String,
        ) {
            every { productService.searchProducts(query, type, any(), any()) } throws exception

            mockMvc.perform(
                get("/api/v1/mobile/products/search")
                    .param("query", query)
                    .param("type", type)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }

        @Test
        @DisplayName("query 파라미터 누락 - 400 에러")
        fun searchProducts_missingQuery_returnsBadRequest() {
            mockMvc.perform(
                get("/api/v1/mobile/products/search")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }
    }

    private fun createProductDto(
        productId: String,
        productName: String,
        productCode: String,
        barcode: String
    ): ProductDto {
        return ProductDto(
            productCode = productCode,
            productName = productName,
            logisticsBarcode = barcode,
            storageCondition = "상온",
            shelfLife = "7개월",
            category1 = "라면",
            category2 = "용기면"
        )
    }

    companion object {
        @JvmStatic
        fun searchExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "shortQuery (1자) -> INVALID_PARAMETER",
                "열",
                "text",
                InvalidSearchParameterException("검색어는 2자 이상이어야 합니다"),
                "INVALID_PARAMETER",
            ),
            Arguments.of(
                "invalidType -> INVALID_SEARCH_TYPE",
                "열라면",
                "invalid",
                InvalidSearchTypeException(),
                "INVALID_SEARCH_TYPE",
            ),
            Arguments.of(
                "invalidBarcode -> INVALID_PARAMETER",
                "abc",
                "barcode",
                InvalidSearchParameterException("유효하지 않은 바코드 형식입니다"),
                "INVALID_PARAMETER",
            ),
        )
    }
}
