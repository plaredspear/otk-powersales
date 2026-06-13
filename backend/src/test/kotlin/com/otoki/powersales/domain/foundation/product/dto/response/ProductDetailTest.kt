package com.otoki.powersales.domain.foundation.product.dto.response

import com.otoki.powersales.domain.foundation.product.dto.response.ProductDetail
import com.otoki.powersales.domain.foundation.product.entity.Product
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProductDetail.from - 이미지 URL 조립")
class ProductDetailTest {

    @Nested
    @DisplayName("imgRefPath* 조립")
    inner class ImageUrlComposition {

        @Test
        @DisplayName("base + front + back 모두 존재 -> 절대 URL 반환")
        fun allPresent() {
            val product = createProduct(
                txt = "https://ottogi-hdrive.s3.ap-northeast-2.amazonaws.com/",
                front = "26620006(F)_오쉐프 해바라기유 18L_정면.jpg",
                back = "26620006(B)_오쉐프 해바라기유 18L_후면.jpg",
            )

            val dto = ProductDetail.from(product)

            // java.net.URI 의 IRI 처리 — 한글은 그대로 두고 path-illegal 문자 (공백 등) 만 percent-encode.
            // 브라우저는 IRI 를 fetch 시점에 UTF-8 percent-encoding 으로 자동 변환하므로 양쪽 모두 작동.
            assertThat(dto.imgRefPathFront)
                .isEqualTo("https://ottogi-hdrive.s3.ap-northeast-2.amazonaws.com/26620006(F)_오쉐프%20해바라기유%2018L_정면.jpg")
            assertThat(dto.imgRefPathBack)
                .isEqualTo("https://ottogi-hdrive.s3.ap-northeast-2.amazonaws.com/26620006(B)_오쉐프%20해바라기유%2018L_후면.jpg")
        }

        @Test
        @DisplayName("back NULL -> back 만 null 반환")
        fun backNull() {
            val product = createProduct(
                txt = "https://ottogi-hdrive.s3.ap-northeast-2.amazonaws.com/",
                front = "26610001(F)_3D)프레스코 해바라기유 500ml_정면(202103).jpg",
                back = null,
            )

            val dto = ProductDetail.from(product)

            assertThat(dto.imgRefPathFront).startsWith("https://ottogi-hdrive.s3.ap-northeast-2.amazonaws.com/")
            assertThat(dto.imgRefPathFront).contains("26610001(F)")
            assertThat(dto.imgRefPathBack).isNull()
        }

        @Test
        @DisplayName("base URL NULL -> front/back 모두 null 반환")
        fun baseNull() {
            val product = createProduct(
                txt = null,
                front = "front.jpg",
                back = "back.jpg",
            )

            val dto = ProductDetail.from(product)

            assertThat(dto.imgRefPathFront).isNull()
            assertThat(dto.imgRefPathBack).isNull()
        }

        @Test
        @DisplayName("base URL 빈문자열 -> null 반환")
        fun baseBlank() {
            val product = createProduct(txt = "  ", front = "front.jpg", back = null)

            val dto = ProductDetail.from(product)

            assertThat(dto.imgRefPathFront).isNull()
        }

        @Test
        @DisplayName("base URL trailing slash 없음 + relative leading slash 없음 -> slash 1개로 연결")
        fun slashNormalization() {
            val product = createProduct(txt = "https://example.com", front = "path/img.jpg", back = null)

            val dto = ProductDetail.from(product)

            assertThat(dto.imgRefPathFront).isEqualTo("https://example.com/path/img.jpg")
        }

        @Test
        @DisplayName("base URL trailing slash + relative leading slash 동시 -> slash 중복 회피")
        fun slashDuplicateAvoided() {
            val product = createProduct(txt = "https://example.com/", front = "/path/img.jpg", back = null)

            val dto = ProductDetail.from(product)

            assertThat(dto.imgRefPathFront).isEqualTo("https://example.com/path/img.jpg")
        }
    }

    private fun createProduct(
        txt: String?,
        front: String?,
        back: String?,
    ): Product = Product(
        id = 1L,
        productCode = "P001",
        name = "테스트",
        imgRefPathTxt = txt,
        imgRefPathFront = front,
        imgRefPathBack = back,
    )
}
