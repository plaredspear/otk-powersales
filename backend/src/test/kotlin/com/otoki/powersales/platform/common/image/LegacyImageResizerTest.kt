package com.otoki.powersales.platform.common.image

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * 레거시 Heroku `ImageUtil.resizeImage(650, 650)` + `getNewPath()` 재현 검증.
 *
 * SF 는 이 축소본의 픽셀·용량·파일명을 그대로 `UploadFile__c` 에 담아 보여주므로, 결과가 레거시와
 * 어긋나면 SF 화면에 뜨는 이미지가 달라진다.
 */
@DisplayName("LegacyImageResizer - 레거시 650x650 축소 재현")
class LegacyImageResizerTest {

    private fun imageBytes(width: Int, height: Int, format: String = "jpg"): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, format, out)
        return out.toByteArray()
    }

    private fun dimensionsOf(bytes: ByteArray): Pair<Int, Int> {
        val image = ImageIO.read(ByteArrayInputStream(bytes))
        return image.width to image.height
    }

    @Nested
    @DisplayName("축소 비율 — 레거시 순차 적용 + int 절삭")
    inner class Scaling {

        @Test
        fun `가로가 긴 이미지는 가로 650 기준으로 축소된다`() {
            val result = LegacyImageResizer.resize(imageBytes(1300, 650), "photo.jpg")

            assertThat(result.resized).isTrue()
            // 1300 → ratio 0.5 → 650 x 325
            assertThat(dimensionsOf(result.bytes)).isEqualTo(650 to 325)
        }

        @Test
        fun `세로가 긴 이미지는 세로 650 기준으로 축소된다`() {
            val result = LegacyImageResizer.resize(imageBytes(650, 1300), "photo.jpg")

            assertThat(dimensionsOf(result.bytes)).isEqualTo(325 to 650)
        }

        @Test
        fun `양쪽이 모두 큰 이미지는 width 판정 후 갱신된 height 로 재판정한다`() {
            // 2000x1500 → width 분기: ratio 0.325 → 650 x 487(int 절삭)
            // → height(487) 는 650 이하라 두 번째 분기 미진입.
            val result = LegacyImageResizer.resize(imageBytes(2000, 1500), "photo.jpg")

            assertThat(dimensionsOf(result.bytes)).isEqualTo(650 to 487)
        }

        @Test
        fun `650 이하 이미지는 확대하지 않는다 — 레거시는 초과일 때만 비율 적용`() {
            val result = LegacyImageResizer.resize(imageBytes(300, 200), "photo.jpg")

            assertThat(dimensionsOf(result.bytes)).isEqualTo(300 to 200)
        }
    }

    @Nested
    @DisplayName("파일명 — 레거시 _resize 접미")
    inner class FileName {

        @Test
        fun `확장자 앞에 _resize 를 붙인다`() {
            val result = LegacyImageResizer.resize(imageBytes(100, 100), "photo.jpg")

            assertThat(result.fileName).isEqualTo("photo_resize.jpg")
        }

        @Test
        fun `확장자 문자열이 이름에 섞여 있어도 마지막 확장자만 치환한다`() {
            // 레거시 replaceFirst("."+ext, ...) 는 정규식이라 "m_resize.jpgfile.jpg" 로 망가진다 — 미재현.
            val result = LegacyImageResizer.resize(imageBytes(100, 100), "myjpgfile.jpg")

            assertThat(result.fileName).isEqualTo("myjpgfile_resize.jpg")
        }

        @Test
        fun `윈도우 전체 경로로 와도 파일명만 취한다 (레거시 IE 보정 정합)`() {
            val result = LegacyImageResizer.resize(imageBytes(100, 100), "C:\\temp\\photo.jpg")

            assertThat(result.fileName).isEqualTo("photo_resize.jpg")
        }
    }

    @Nested
    @DisplayName("디코딩 불가 — 원본 유지 fallback")
    inner class Fallback {

        @Test
        fun `이미지가 아닌 바이트는 원본과 원본 파일명을 그대로 돌려준다`() {
            val raw = byteArrayOf(1, 2, 3, 4)

            val result = LegacyImageResizer.resize(raw, "broken.heic")

            assertThat(result.resized).isFalse()
            assertThat(result.bytes).isEqualTo(raw)
            assertThat(result.fileName).isEqualTo("broken.heic")
        }

        @Test
        fun `확장자가 없으면 리사이즈하지 않는다`() {
            val raw = imageBytes(100, 100)

            val result = LegacyImageResizer.resize(raw, "noext")

            assertThat(result.resized).isFalse()
            assertThat(result.fileName).isEqualTo("noext")
        }
    }
}
