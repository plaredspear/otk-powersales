package com.otoki.powersales.external.sap.outbound.guard

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

@DisplayName("SapResponseInterpreter 확정 거부(rejected) 판별 테스트")
class SapResponseInterpreterTest {

    private val objectMapper = ObjectMapper()

    @Test
    @DisplayName("resultCode='S' → 성공, rejected=false")
    fun success() {
        val result = SapResponseInterpreter.interpret(objectMapper, """{"resultCode":"S","resutlMsg":"OK"}""")

        assertThat(result.success).isTrue
        assertThat(result.rejected).isFalse
        assertThat(result.resultCodeRaw).isEqualTo("S")
    }

    @Test
    @DisplayName("resultCode='E' → 실패 + 확정 거부(rejected=true), 사유는 resutlMsg 원문")
    fun explicitReject() {
        val result = SapResponseInterpreter.interpret(objectMapper, """{"resultCode":"E","resutlMsg":"여신 한도 초과"}""")

        assertThat(result.success).isFalse
        assertThat(result.rejected).isTrue
        assertThat(result.message).isEqualTo("여신 한도 초과")
    }

    @Test
    @DisplayName("resultCode 가 'S'/'E' 아닌 임의 값이어도 rejected=true (S 아닌 모든 값)")
    fun nonStandardCodeIsRejected() {
        val result = SapResponseInterpreter.interpret(objectMapper, """{"resultCode":"X","resultMsg":"알 수 없음"}""")

        assertThat(result.success).isFalse
        assertThat(result.rejected).isTrue
        assertThat(result.resultCodeRaw).isEqualTo("X")
    }

    @Test
    @DisplayName("빈 응답 → 실패지만 rejected=false (resultCode 없음, 재시도 대상)")
    fun emptyBodyIsNotRejected() {
        val result = SapResponseInterpreter.interpret(objectMapper, "")

        assertThat(result.success).isFalse
        assertThat(result.rejected).isFalse
        assertThat(result.message).isEqualTo("EMPTY_RESPONSE")
        assertThat(result.resultCodeRaw).isNull()
    }

    @Test
    @DisplayName("JSON 파싱 실패 → 실패지만 rejected=false (resultCode 없음, 재시도 대상)")
    fun invalidJsonIsNotRejected() {
        val result = SapResponseInterpreter.interpret(objectMapper, "not-json")

        assertThat(result.success).isFalse
        assertThat(result.rejected).isFalse
        assertThat(result.resultCodeRaw).isNull()
    }

    @Test
    @DisplayName("resultCode 필드 자체가 없는 JSON → rejected=false (재시도 대상)")
    fun missingResultCodeIsNotRejected() {
        val result = SapResponseInterpreter.interpret(objectMapper, """{"foo":"bar"}""")

        assertThat(result.success).isFalse
        assertThat(result.rejected).isFalse
        assertThat(result.resultCodeRaw).isNull()
    }
}
