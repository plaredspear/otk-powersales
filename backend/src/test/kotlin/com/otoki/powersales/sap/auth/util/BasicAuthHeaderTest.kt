package com.otoki.powersales.sap.auth.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.Base64

@DisplayName("BasicAuthHeader 테스트")
class BasicAuthHeaderTest {

    private fun encode(value: String): String =
        Base64.getEncoder().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    @Nested
    @DisplayName("parse - 정상 케이스")
    inner class Success {

        @Test
        @DisplayName("표준 형식 - id:secret -> 디코드 성공")
        fun standardForm() {
            val header = "Basic " + encode("otoki-sap:supersecret")

            val result = BasicAuthHeader.parse(header)

            assertThat(result).isNotNull
            assertThat(result!!.clientId).isEqualTo("otoki-sap")
            assertThat(result.clientSecret).isEqualTo("supersecret")
        }

        @Test
        @DisplayName("scheme 대소문자 - basic / BASIC -> 모두 디코드")
        fun schemeCaseInsensitive() {
            val encoded = encode("u:p")

            assertThat(BasicAuthHeader.parse("basic $encoded")?.clientId).isEqualTo("u")
            assertThat(BasicAuthHeader.parse("BASIC $encoded")?.clientId).isEqualTo("u")
            assertThat(BasicAuthHeader.parse("BaSiC $encoded")?.clientId).isEqualTo("u")
        }

        @Test
        @DisplayName("secret 안에 콜론 포함 - 첫 번째 ':' 으로만 split")
        fun secretContainsColon() {
            val header = "Basic " + encode("client-id:secret:with:colons")

            val result = BasicAuthHeader.parse(header)

            assertThat(result!!.clientId).isEqualTo("client-id")
            assertThat(result.clientSecret).isEqualTo("secret:with:colons")
        }

        @Test
        @DisplayName("URL-encoded 특수문자 (콜론 인코딩) - 디코드 후 정상 복원")
        fun urlEncodedColon() {
            // RFC 6749 §2.3.1: client_id / client_secret 는 application/x-www-form-urlencoded 인코딩 후 base64
            // ':' 가 secret 에 포함된 경우 사전에 %3A 로 percent-encode 해야 split 모호성 회피 가능.
            val header = "Basic " + encode("client%3Aid:secret%3Avalue")

            val result = BasicAuthHeader.parse(header)

            assertThat(result!!.clientId).isEqualTo("client:id")
            assertThat(result.clientSecret).isEqualTo("secret:value")
        }

        @Test
        @DisplayName("URL-encoded 한글/공백 - 디코드 후 정상 복원")
        fun urlEncodedUnicode() {
            // "한글:패스 워드" 의 percent-encoded 표현
            val raw = "%ED%95%9C%EA%B8%80:%ED%8C%A8%EC%8A%A4+%EC%9B%8C%EB%93%9C"
            val header = "Basic " + encode(raw)

            val result = BasicAuthHeader.parse(header)

            assertThat(result!!.clientId).isEqualTo("한글")
            assertThat(result.clientSecret).isEqualTo("패스 워드")
        }

        @Test
        @DisplayName("secret 빈 문자열 (id 만 있음) - 디코드 성공 + secret=빈문자열")
        fun emptySecret() {
            val header = "Basic " + encode("only-id:")

            val result = BasicAuthHeader.parse(header)

            assertThat(result!!.clientId).isEqualTo("only-id")
            assertThat(result.clientSecret).isEqualTo("")
        }
    }

    @Nested
    @DisplayName("parse - 손상 / 부재 케이스 -> null 반환")
    inner class Invalid {

        @Test
        @DisplayName("헤더 null -> null")
        fun headerNull() {
            assertThat(BasicAuthHeader.parse(null)).isNull()
        }

        @Test
        @DisplayName("헤더 빈문자열/공백 -> null")
        fun headerBlank() {
            assertThat(BasicAuthHeader.parse("")).isNull()
            assertThat(BasicAuthHeader.parse("   ")).isNull()
        }

        @Test
        @DisplayName("scheme 다름 (Bearer 등) -> null")
        fun nonBasicScheme() {
            assertThat(BasicAuthHeader.parse("Bearer xyz")).isNull()
            assertThat(BasicAuthHeader.parse("Digest abc")).isNull()
        }

        @Test
        @DisplayName("Basic 뒤 base64 누락 -> null")
        fun missingPayload() {
            assertThat(BasicAuthHeader.parse("Basic")).isNull()
            assertThat(BasicAuthHeader.parse("Basic ")).isNull()
            assertThat(BasicAuthHeader.parse("Basic    ")).isNull()
        }

        @Test
        @DisplayName("잘못된 base64 -> null")
        fun malformedBase64() {
            assertThat(BasicAuthHeader.parse("Basic !!!not-base64!!!")).isNull()
        }

        @Test
        @DisplayName("디코드 결과에 ':' 없음 -> null")
        fun noColonInDecoded() {
            val header = "Basic " + encode("no-colon-here")

            assertThat(BasicAuthHeader.parse(header)).isNull()
        }

        @Test
        @DisplayName("URL-decode 실패 (잘못된 % 인코딩) -> null")
        fun malformedPercentEncoding() {
            // 'XYZ' 부분이 hex 가 아니라 URLDecoder.decode() 가 IllegalArgumentException 발생
            val header = "Basic " + encode("%XYZ:secret")

            assertThat(BasicAuthHeader.parse(header)).isNull()
        }
    }
}
