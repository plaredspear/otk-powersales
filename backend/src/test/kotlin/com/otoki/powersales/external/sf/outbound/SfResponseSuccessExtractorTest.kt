package com.otoki.powersales.external.sf.outbound

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

@DisplayName("SfResponseSuccessExtractor 테스트")
class SfResponseSuccessExtractorTest {

    private val extractor = SfResponseSuccessExtractor(JsonMapper.builder().build())

    @Test
    @DisplayName("RESULT_CODE=200 이면 성공 판정")
    fun resultCodeSuccess() {
        val verdict = extractor.resolve(200, """{"RESULT_CODE":"200","RESULT_MSG":"성공"}""")
        assertThat(verdict).isNotNull
        assertThat(verdict!!.success).isTrue()
        assertThat(verdict.errorMessage).isNull()
    }

    @Test
    @DisplayName("HTTP 200 이어도 RESULT_CODE!=200 이면 실패 판정 + RESULT_MSG 를 사유로")
    fun resultCodeFailureOnHttp200() {
        val verdict = extractor.resolve(200, """{"RESULT_CODE":"500","RESULT_MSG":"필수값 누락"}""")
        assertThat(verdict).isNotNull
        assertThat(verdict!!.success).isFalse()
        assertThat(verdict.errorMessage).isEqualTo("RESULT_CODE=500: 필수값 누락")
    }

    @Test
    @DisplayName("RESULT_MSG 가 비어도 RESULT_CODE 는 사유에 남긴다")
    fun failureWithBlankMsg() {
        val verdict = extractor.resolve(200, """{"RESULT_CODE":"E","RESULT_MSG":""}""")
        assertThat(verdict!!.success).isFalse()
        assertThat(verdict.errorMessage).isEqualTo("RESULT_CODE=E")
    }

    @Test
    @DisplayName("RESULT_CODE 가 없는 응답(fetch 목록 등)은 null — HTTP 판정 fallback")
    fun noResultCodeReturnsNull() {
        assertThat(extractor.resolve(200, """{"Result":[{"a":1},{"a":2}]}""")).isNull()
        assertThat(extractor.resolve(200, """[{"a":1}]""")).isNull()
    }

    @Test
    @DisplayName("non-2xx HTTP status 는 body 재판정 없이 null — 기존 HTTP 실패 판정 존중")
    fun nonSuccessHttpReturnsNull() {
        assertThat(extractor.resolve(500, """{"RESULT_CODE":"500"}""")).isNull()
        assertThat(extractor.resolve(401, """{"RESULT_CODE":"200"}""")).isNull()
        assertThat(extractor.resolve(null, """{"RESULT_CODE":"200"}""")).isNull()
    }

    @Test
    @DisplayName("null/blank/파싱 불가 응답은 null")
    fun nullOrBlankOrUnparseable() {
        assertThat(extractor.resolve(200, null)).isNull()
        assertThat(extractor.resolve(200, "")).isNull()
        assertThat(extractor.resolve(200, "   ")).isNull()
        assertThat(extractor.resolve(200, "not a json")).isNull()
    }
}
