package com.otoki.powersales.domain.foundation.product.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.foundation.product.dto.response.OrderProductDto
import com.otoki.powersales.domain.foundation.product.service.FavoriteProductService
import com.otoki.powersales.platform.common.exception.AlreadyFavoritedException
import com.otoki.powersales.platform.common.exception.FavoriteNotFoundException
import com.otoki.powersales.platform.common.exception.ProductNotFoundException
import com.otoki.powersales.platform.common.test.MobileControllerTestSupport
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(FavoriteProductController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FavoriteProductController 테스트")
class FavoriteProductControllerTest : MobileControllerTestSupport() {

    @MockkBean
    private lateinit var favoriteProductService: FavoriteProductService

    private fun sampleProduct(code: String, name: String) = OrderProductDto(
        productCode = code,
        productName = name,
        barcode = "880$code",
        storageType = "실온",
        shelfLife = "7개월",
        unitPrice = 1200,
        boxSize = 30,
        isFavorite = true,
        categoryMid = "라면",
        categorySub = "가정",
        productType = null,
        tasteGiftType = null,
    )

    @Nested
    @DisplayName("GET /api/v1/mobile/me/favorite-products")
    inner class GetMyFavorites {

        @Test
        @DisplayName("성공 — 즐겨찾기 목록 반환(isFavorite=true)")
        fun success() {
            every { favoriteProductService.getMyFavoriteProducts(1L) } returns listOf(
                sampleProduct("P1", "열라면"),
                sampleProduct("P2", "참깨라면"),
            )

            mockMvc.perform(get("/api/v1/mobile/me/favorite-products"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].productCode").value("P1"))
                .andExpect(jsonPath("$.data[0].isFavorite").value(true))
        }

        @Test
        @DisplayName("빈 목록 — 빈 배열 반환")
        fun empty() {
            every { favoriteProductService.getMyFavoriteProducts(1L) } returns emptyList()

            mockMvc.perform(get("/api/v1/mobile/me/favorite-products"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(0))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/mobile/me/favorite-products/{productCode}")
    inner class AddFavorite {

        @Test
        @DisplayName("성공 — 200 + 메시지")
        fun success() {
            every { favoriteProductService.addFavoriteProduct(1L, "P1") } just Runs

            mockMvc.perform(post("/api/v1/mobile/me/favorite-products/P1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("즐겨찾기에 추가되었습니다"))
            verify { favoriteProductService.addFavoriteProduct(1L, "P1") }
        }

        @Test
        @DisplayName("이미 즐겨찾기 — 400 ALREADY_FAVORITED")
        fun alreadyFavorited() {
            every { favoriteProductService.addFavoriteProduct(1L, "P1") } throws AlreadyFavoritedException()

            mockMvc.perform(post("/api/v1/mobile/me/favorite-products/P1"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ALREADY_FAVORITED"))
        }

        @Test
        @DisplayName("제품 없음 — 404 PRODUCT_NOT_FOUND")
        fun productNotFound() {
            every { favoriteProductService.addFavoriteProduct(1L, "X") } throws ProductNotFoundException("X")

            mockMvc.perform(post("/api/v1/mobile/me/favorite-products/X"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/mobile/me/favorite-products/{productCode}")
    inner class RemoveFavorite {

        @Test
        @DisplayName("성공 — 200 + 메시지")
        fun success() {
            every { favoriteProductService.removeFavoriteProduct(1L, "P1") } just Runs

            mockMvc.perform(delete("/api/v1/mobile/me/favorite-products/P1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.message").value("즐겨찾기에서 해제되었습니다"))
            verify { favoriteProductService.removeFavoriteProduct(1L, "P1") }
        }

        @Test
        @DisplayName("즐겨찾기 없음 — 404 FAVORITE_NOT_FOUND")
        fun notFound() {
            every { favoriteProductService.removeFavoriteProduct(1L, "P1") } throws FavoriteNotFoundException()

            mockMvc.perform(delete("/api/v1/mobile/me/favorite-products/P1"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("FAVORITE_NOT_FOUND"))
        }
    }
}
