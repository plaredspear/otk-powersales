package com.otoki.powersales.external.sap.auth.util

import com.otoki.powersales.external.sap.auth.util.IpAllowlistMatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("IpAllowlistMatcher 테스트")
class IpAllowlistMatcherTest {

    @Nested
    @DisplayName("isEnabled - allowlist 활성 여부")
    inner class IsEnabled {

        @Test
        @DisplayName("빈 리스트 - 비활성 -> isEnabled=false")
        fun emptyList_disabled() {
            val matcher = IpAllowlistMatcher(emptyList())
            assertThat(matcher.isEnabled).isFalse()
        }

        @Test
        @DisplayName("blank 항목만 - 비활성 -> isEnabled=false")
        fun blankOnly_disabled() {
            val matcher = IpAllowlistMatcher(listOf("", "  "))
            assertThat(matcher.isEnabled).isFalse()
        }

        @Test
        @DisplayName("CIDR 1개 - 활성 -> isEnabled=true")
        fun oneCidr_enabled() {
            val matcher = IpAllowlistMatcher(listOf("203.0.113.0/24"))
            assertThat(matcher.isEnabled).isTrue()
        }
    }

    @Nested
    @DisplayName("matches - IP 매칭")
    inner class Matches {

        @Test
        @DisplayName("비활성 - 어떤 IP 든 통과")
        fun disabled_alwaysPass() {
            val matcher = IpAllowlistMatcher(emptyList())
            assertThat(matcher.matches("1.2.3.4")).isTrue()
            assertThat(matcher.matches(null)).isTrue()
        }

        @Test
        @DisplayName("CIDR 범위 내 - 통과")
        fun inCidr_pass() {
            val matcher = IpAllowlistMatcher(listOf("203.0.113.0/24"))
            assertThat(matcher.matches("203.0.113.10")).isTrue()
        }

        @Test
        @DisplayName("CIDR 범위 외 - 차단")
        fun outOfCidr_blocked() {
            val matcher = IpAllowlistMatcher(listOf("203.0.113.0/24"))
            assertThat(matcher.matches("198.51.100.1")).isFalse()
        }

        @Test
        @DisplayName("단일 IP - 정확 매칭")
        fun singleIp_exactMatch() {
            val matcher = IpAllowlistMatcher(listOf("198.51.100.7/32"))
            assertThat(matcher.matches("198.51.100.7")).isTrue()
            assertThat(matcher.matches("198.51.100.8")).isFalse()
        }

        @Test
        @DisplayName("활성인데 IP 없음 - 차단")
        fun enabledButNoIp_blocked() {
            val matcher = IpAllowlistMatcher(listOf("203.0.113.0/24"))
            assertThat(matcher.matches(null)).isFalse()
            assertThat(matcher.matches("")).isFalse()
        }
    }
}
