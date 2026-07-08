package com.otoki.powersales.external.sf.outbound

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

@DisplayName("SfResponseCountExtractor 테스트")
class SfResponseCountExtractorTest {

    private val extractor = SfResponseCountExtractor(JsonMapper.builder().build())

    @Test
    @DisplayName("최상위가 배열이면 배열 크기를 반환")
    fun topLevelArray() {
        assertThat(extractor.extract("""[{"a":1},{"a":2}]""")).isEqualTo(2)
    }

    @Test
    @DisplayName("data wrapper 배열 크기를 반환")
    fun dataWrapper() {
        assertThat(extractor.extract("""{"data":[{"a":1},{"a":2},{"a":3}]}""")).isEqualTo(3)
    }

    @Test
    @DisplayName("대문자 wrapper key (RESULT) 도 인식")
    fun uppercaseWrapper() {
        assertThat(extractor.extract("""{"RESULT":[{"a":1}]}""")).isEqualTo(1)
    }

    @Test
    @DisplayName("빈 배열은 0 을 반환")
    fun emptyArray() {
        assertThat(extractor.extract("""{"data":[]}""")).isEqualTo(0)
    }

    @Test
    @DisplayName("배열이 없는 응답(등록 성공 단일 형식)은 null")
    fun noArrayResponse() {
        assertThat(extractor.extract("""{"RESULT_CODE":"200","RESULT_MSG":"성공"}""")).isNull()
    }

    @Test
    @DisplayName("null/blank 응답은 null")
    fun nullOrBlank() {
        assertThat(extractor.extract(null)).isNull()
        assertThat(extractor.extract("")).isNull()
        assertThat(extractor.extract("   ")).isNull()
    }

    @Test
    @DisplayName("파싱 불가 응답(JSON 아님)은 null")
    fun unparseable() {
        assertThat(extractor.extract("not a json")).isNull()
    }
}
