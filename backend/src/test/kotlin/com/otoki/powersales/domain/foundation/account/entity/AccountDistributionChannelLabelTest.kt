package com.otoki.powersales.domain.foundation.account.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Account.distributionChannelLabel — 유통형태 조합")
class AccountDistributionChannelLabelTest {

    @Test
    @DisplayName("거래처상태코드 + 거래처유형 을 공백으로 조합")
    fun combinesStatusCodeAndType() {
        val account = Account(
            accountStatusCode = "02",
            accountType = "슈퍼",
        )

        assertThat(account.distributionChannelLabel()).isEqualTo("02 슈퍼")
    }

    @Test
    @DisplayName("거래처상태코드만 있으면 코드만 반환")
    fun statusCodeOnly() {
        val account = Account(accountStatusCode = "02", accountType = null)

        assertThat(account.distributionChannelLabel()).isEqualTo("02")
    }

    @Test
    @DisplayName("거래처유형만 있으면 라벨만 반환")
    fun typeOnly() {
        val account = Account(accountStatusCode = null, accountType = "체인")

        assertThat(account.distributionChannelLabel()).isEqualTo("체인")
    }

    @Test
    @DisplayName("둘 다 없으면 null")
    fun bothNull_returnsNull() {
        val account = Account(accountStatusCode = null, accountType = null)

        assertThat(account.distributionChannelLabel()).isNull()
    }

    @Test
    @DisplayName("빈 문자열 거래처상태코드는 제외")
    fun blankStatusCode_excluded() {
        val account = Account(accountStatusCode = "  ", accountType = "체인")

        assertThat(account.distributionChannelLabel()).isEqualTo("체인")
    }
}
