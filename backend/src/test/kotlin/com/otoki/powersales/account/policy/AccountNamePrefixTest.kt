package com.otoki.powersales.account.policy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("AccountNamePrefix 테스트 (Spec #640 P1-B)")
class AccountNamePrefixTest {

    @ParameterizedTest(name = "isValidName(\"{0}\") = {1}")
    @CsvSource(
        "(신규) 강남점,           true",
        "(기타) 외부거래처,       true",
        "강남점,                  false",
        "(신규)(기타) test,        true",
        "(신규)앞공백없음,        true",
        "신규 강남점,             false",
        "'(',                     false",
        "신규,                    false"
    )
    @DisplayName("진리표 — ALLOWED prefix contains 검증")
    fun truthTable(name: String, expected: Boolean) {
        assertThat(AccountNamePrefix.isValidName(name)).isEqualTo(expected)
    }

    @Test
    @DisplayName("ALLOWED 초기값 = '(신규)', '(기타)' (레거시 Custom Label 인계)")
    fun allowedInitial() {
        assertThat(AccountNamePrefix.ALLOWED).containsExactly("(신규)", "(기타)")
    }

    @Test
    @DisplayName("joinForMessage - '/' 로 join → '(신규)/(기타)'")
    fun joinForMessage() {
        assertThat(AccountNamePrefix.joinForMessage()).isEqualTo("(신규)/(기타)")
    }
}
