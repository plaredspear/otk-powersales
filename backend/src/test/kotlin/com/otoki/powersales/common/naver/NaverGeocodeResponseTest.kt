package com.otoki.powersales.common.naver

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NaverGeocodeResponse JSON 파싱 (#637)")
class NaverGeocodeResponseTest {

    private val objectMapper = ObjectMapper()

    @Test
    @DisplayName("정상 응답 — addresses[0].x / y 추출")
    fun parses_validResponse() {
        val json = """
            {
              "status": "OK",
              "meta": { "totalCount": 1, "page": 1, "count": 1 },
              "addresses": [
                {
                  "roadAddress": "서울특별시 강남구 테헤란로 123",
                  "jibunAddress": "서울특별시 강남구 역삼동 123-45",
                  "x": "127.0584",
                  "y": "37.5074"
                }
              ],
              "errorMessage": ""
            }
        """.trimIndent()

        val response = objectMapper.readValue(json, NaverGeocodeResponse::class.java)

        assertThat(response.addresses).hasSize(1)
        assertThat(response.addresses[0].x).isEqualTo("127.0584")
        assertThat(response.addresses[0].y).isEqualTo("37.5074")
    }

    @Test
    @DisplayName("addresses 빈 배열")
    fun parses_emptyAddresses() {
        val json = """{ "status": "OK", "addresses": [] }"""

        val response = objectMapper.readValue(json, NaverGeocodeResponse::class.java)

        assertThat(response.addresses).isEmpty()
    }

    @Test
    @DisplayName("미지의 응답 필드 무시")
    fun parses_unknownFieldsIgnored() {
        val json = """
            {
              "addresses": [{ "x": "127.1", "y": "37.5", "unknownField": "ignore-me" }],
              "newField": "ignore-me"
            }
        """.trimIndent()

        val response = objectMapper.readValue(json, NaverGeocodeResponse::class.java)

        assertThat(response.addresses).hasSize(1)
        assertThat(response.addresses[0].x).isEqualTo("127.1")
    }
}
