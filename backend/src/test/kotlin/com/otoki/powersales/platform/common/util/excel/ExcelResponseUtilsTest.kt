package com.otoki.powersales.platform.common.util.excel

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

@DisplayName("ExcelResponseUtils 테스트")
class ExcelResponseUtilsTest {

    @Test
    @DisplayName("한글 파일명 - UTF-8 RFC5987 인코딩된 Content-Disposition 헤더")
    fun build_encodesKoreanFilename() {
        val response = ExcelResponseUtils.build(byteArrayOf(0x50, 0x4B), "행사마스터.xlsx")

        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.headers.contentType.toString()).isEqualTo(XLSX_CONTENT_TYPE)
        val disposition = response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)
        assertThat(disposition).startsWith("attachment; filename*=UTF-8''")
        // URLEncoder 인코딩 — 한글이 %XX 로, 공백은 %20 으로 (+ 아님)
        assertThat(disposition).contains("%")
        assertThat(disposition).doesNotContain("+")
        assertThat(response.body).isEqualTo(byteArrayOf(0x50, 0x4B))
    }

    @Test
    @DisplayName("ExcelResult 오버로드 - bytes/filename 그대로 위임")
    fun build_fromExcelResult() {
        val result = ExcelResult(byteArrayOf(1, 2, 3), "test file.xlsx")
        val response = ExcelResponseUtils.build(result)

        assertThat(response.body).isEqualTo(byteArrayOf(1, 2, 3))
        val disposition = response.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)
        // 공백 → %20
        assertThat(disposition).contains("test%20file.xlsx")
    }
}
