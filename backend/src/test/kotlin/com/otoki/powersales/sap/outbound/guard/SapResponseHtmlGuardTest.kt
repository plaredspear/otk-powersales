package com.otoki.powersales.sap.outbound.guard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SapResponseHtmlGuard — SAP/proxy HTML 응답 가드 (#588 §4.3)")
class SapResponseHtmlGuardTest {

    @Test
    @DisplayName("정상 JSON 응답은 통과")
    fun valid_jsonBody() {
        assertThat(SapResponseHtmlGuard.isValid("""{"resultCode":"200"}""")).isTrue
    }

    @Test
    @DisplayName("`<` 포함 응답은 실패")
    fun invalid_htmlBody() {
        assertThat(SapResponseHtmlGuard.isValid("<html><body>Error</body></html>")).isFalse
        assertThat(SapResponseHtmlGuard.isValid("text < text")).isFalse
    }

    @Test
    @DisplayName("null 또는 빈 본문은 통과 (호출 측 별도 처리)")
    fun nullOrBlank_passes() {
        assertThat(SapResponseHtmlGuard.isValid(null)).isTrue
        assertThat(SapResponseHtmlGuard.isValid("")).isTrue
        assertThat(SapResponseHtmlGuard.isValid("   ")).isTrue
    }
}
