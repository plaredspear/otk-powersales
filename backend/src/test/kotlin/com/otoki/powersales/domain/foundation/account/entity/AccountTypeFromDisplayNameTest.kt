package com.otoki.powersales.domain.foundation.account.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 유통형태 검색은 거래처유형마스터(AccountCategoryMaster.name) 를 [AccountType.fromDisplayNameOrNull]
 * 로 환원해 `Account.accountType` IN 조건을 구성한다. 환원이 깨지면 검색이 조용히 코드 매칭만
 * 하게 되므로, displayName ↔ enum 정합을 보호한다.
 */
@DisplayName("AccountType.fromDisplayNameOrNull — 거래처유형마스터 Name 환원")
class AccountTypeFromDisplayNameTest {

    @Test
    @DisplayName("모든 enum 의 displayName 은 자기 자신으로 환원된다")
    fun everyDisplayNameRoundTrips() {
        AccountType.entries.forEach { type ->
            assertThat(AccountType.fromDisplayNameOrNull(type.displayName))
                .`as`("displayName=${type.displayName}")
                .isEqualTo(type)
        }
    }

    @Test
    @DisplayName("대표 마스터 Name 매칭 — 슈퍼/체인/대형마트(3대)")
    fun matchesRepresentativeNames() {
        assertThat(AccountType.fromDisplayNameOrNull("슈퍼")).isEqualTo(AccountType.SUPER)
        assertThat(AccountType.fromDisplayNameOrNull("체인")).isEqualTo(AccountType.CHAIN)
        assertThat(AccountType.fromDisplayNameOrNull("대형마트(3대)")).isEqualTo(AccountType.DISCOUNT_STORE)
    }

    @Test
    @DisplayName("매칭되지 않는 값/공백/null 은 null")
    fun unmatchedReturnsNull() {
        assertThat(AccountType.fromDisplayNameOrNull("존재하지않는유형")).isNull()
        assertThat(AccountType.fromDisplayNameOrNull("")).isNull()
        assertThat(AccountType.fromDisplayNameOrNull(null)).isNull()
    }
}
