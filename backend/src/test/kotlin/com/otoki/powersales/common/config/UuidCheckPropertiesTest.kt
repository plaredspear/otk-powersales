package com.otoki.powersales.common.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * [UuidCheckProperties] 단위/바인딩 테스트.
 *
 * 특히 yml 키 `app.auth.uuid-check.excluded-employee-codes` 가 코드 프로퍼티
 * `excludedEmployeeCodes` 로 정확히 바인딩되는지를 검증한다 (과거 키가
 * `excluded-employee-numbers` 로 어긋나 예외 사번 면제가 무력화된 회귀 방지).
 */
@DisplayName("UuidCheckProperties")
class UuidCheckPropertiesTest {

    @Nested
    @DisplayName("isExcluded - 면제 사번 판정")
    inner class IsExcluded {

        @Test
        @DisplayName("빈 설정이면 어떤 사번도 면제되지 않는다")
        fun blank_excludesNone() {
            val props = UuidCheckProperties(excludedEmployeeCodes = "")

            assertThat(props.isExcluded("00000009")).isFalse()
        }

        @Test
        @DisplayName("CSV 에 포함된 사번은 면제, 미포함 사번은 비면제")
        fun csv_matchesMember() {
            val props = UuidCheckProperties(excludedEmployeeCodes = "00000009,20030239")

            assertThat(props.isExcluded("00000009")).isTrue()
            assertThat(props.isExcluded("20030239")).isTrue()
            assertThat(props.isExcluded("99999999")).isFalse()
        }

        @Test
        @DisplayName("CSV 항목 앞뒤 공백은 trim 되어 비교된다")
        fun csv_trimsWhitespace() {
            val props = UuidCheckProperties(excludedEmployeeCodes = " 00000009 , 20030239 ")

            assertThat(props.isExcluded("00000009")).isTrue()
            assertThat(props.isExcluded("20030239")).isTrue()
        }
    }

    @Nested
    @DisplayName("프로퍼티 바인딩")
    inner class Binding {

        private val runner = ApplicationContextRunner()
            .withUserConfiguration(UuidCheckPropertiesTestConfig::class.java)

        @Test
        @DisplayName("excluded-employee-codes 키가 excludedEmployeeCodes 로 바인딩된다")
        fun bindsExcludedEmployeeCodes() {
            runner.withPropertyValues(
                "app.auth.uuid-check.excluded-employee-codes=00000009,20030239"
            ).run { ctx ->
                val props = ctx.getBean(UuidCheckProperties::class.java)
                assertThat(props.excludedEmployeeCodes).isEqualTo("00000009,20030239")
                assertThat(props.isExcluded("00000009")).isTrue()
                assertThat(props.isExcluded("20030239")).isTrue()
            }
        }

        @Test
        @DisplayName("enabled 키가 바인딩된다 (기본 true)")
        fun bindsEnabled() {
            runner.run { ctx ->
                assertThat(ctx.getBean(UuidCheckProperties::class.java).enabled).isTrue()
            }

            runner.withPropertyValues("app.auth.uuid-check.enabled=false").run { ctx ->
                assertThat(ctx.getBean(UuidCheckProperties::class.java).enabled).isFalse()
            }
        }
    }
}

@EnableConfigurationProperties(UuidCheckProperties::class)
private class UuidCheckPropertiesTestConfig
