package com.otoki.powersales.domain.foundation.account.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Account.abcTypeLabel — 거래처유형 조합")
class AccountAbcTypeLabelTest {

    @Test
    @DisplayName("ABC유형코드 + ABC유형 을 공백으로 조합")
    fun combinesCodeAndType() {
        val account = Account(abcTypeCode = "6111", abcType = "이마트")

        assertThat(account.abcTypeLabel()).isEqualTo("6111 이마트")
    }

    @Test
    @DisplayName("ABC유형코드만 있으면 코드만 반환")
    fun codeOnly() {
        val account = Account(abcTypeCode = "6111", abcType = null)

        assertThat(account.abcTypeLabel()).isEqualTo("6111")
    }

    @Test
    @DisplayName("ABC유형만 있으면 라벨만 반환")
    fun typeOnly() {
        val account = Account(abcTypeCode = null, abcType = "이마트")

        assertThat(account.abcTypeLabel()).isEqualTo("이마트")
    }

    @Test
    @DisplayName("둘 다 없으면 null")
    fun bothNull_returnsNull() {
        val account = Account(abcTypeCode = null, abcType = null)

        assertThat(account.abcTypeLabel()).isNull()
    }

    @Test
    @DisplayName("빈 문자열 ABC유형코드는 제외")
    fun blankCode_excluded() {
        val account = Account(abcTypeCode = "  ", abcType = "이마트")

        assertThat(account.abcTypeLabel()).isEqualTo("이마트")
    }
}
