package com.otoki.powersales.schedule.policy

import com.otoki.powersales.domain.foundation.account.entity.Account
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AbcExemptPolicy 테스트 (Spec #586)")
class AbcExemptPolicyTest {

    @Nested
    @DisplayName("evaluate - 면제 평가")
    inner class EvaluateTests {

        @Test
        @DisplayName("면제 코드 '1110' → skipped=true, reason=ABC_EXEMPT")
        fun evaluate_exemptCode() {
            val account = Account(abcTypeCode = "1110")

            val result = AbcExemptPolicy.evaluate(account)

            assertThat(result.skipped).isTrue()
            assertThat(result.reason).isEqualTo("ABC_EXEMPT")
        }

        @Test
        @DisplayName("E6 — abcTypeCode=null → skipped=false, reason=null")
        fun evaluate_e6_nullCode() {
            val account = Account(abcTypeCode = null)

            val result = AbcExemptPolicy.evaluate(account)

            assertThat(result.skipped).isFalse()
            assertThat(result.reason).isNull()
        }

        @Test
        @DisplayName("E7 — abcTypeCode='' → skipped=false, reason=null")
        fun evaluate_e7_emptyCode() {
            val account = Account(abcTypeCode = "")

            val result = AbcExemptPolicy.evaluate(account)

            assertThat(result.skipped).isFalse()
            assertThat(result.reason).isNull()
        }

        @Test
        @DisplayName("비면제 코드 '9999' → skipped=false")
        fun evaluate_nonExemptCode() {
            val account = Account(abcTypeCode = "9999")

            val result = AbcExemptPolicy.evaluate(account)

            assertThat(result.skipped).isFalse()
            assertThat(result.reason).isNull()
        }

        @Test
        @DisplayName("account=null → skipped=false")
        fun evaluate_nullAccount() {
            val result = AbcExemptPolicy.evaluate(null)

            assertThat(result.skipped).isFalse()
            assertThat(result.reason).isNull()
        }
    }
}
