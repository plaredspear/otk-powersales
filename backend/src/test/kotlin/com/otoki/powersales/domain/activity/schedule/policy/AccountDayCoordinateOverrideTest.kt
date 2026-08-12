package com.otoki.powersales.domain.activity.schedule.policy

import com.otoki.powersales.domain.foundation.account.entity.Account
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.DayOfWeek

@DisplayName("AccountDayCoordinateOverride 테스트 — 이동매장 요일별 좌표 예외")
class AccountDayCoordinateOverrideTest {

    private val jmartExternalKey = "1015773"

    @Nested
    @DisplayName("resolve - 예외 좌표 조회")
    inner class ResolveTests {

        @Test
        @DisplayName("제이마트(1015773) + 수요일 -> 양구점 좌표 반환")
        fun resolve_jmartWednesday_returnsYangguCoordinate() {
            val account = Account(externalKey = jmartExternalKey)

            val result = AccountDayCoordinateOverride.resolve(account, DayOfWeek.WEDNESDAY)

            assertThat(result).isNotNull
            assertThat(result!!.latitude).isCloseTo(38.1018113, within(0.0000001))
            assertThat(result.longitude).isCloseTo(127.9886619, within(0.0000001))
            assertThat(result.label).isEqualTo("제이마트 양구점")
        }

        @ParameterizedTest
        @EnumSource(
            value = DayOfWeek::class,
            names = ["WEDNESDAY"],
            mode = EnumSource.Mode.EXCLUDE
        )
        @DisplayName("제이마트(1015773) + 수요일 외 요일 -> null (거래처 원본 좌표 사용)")
        fun resolve_jmartNonWednesday_returnsNull(day: DayOfWeek) {
            val account = Account(externalKey = jmartExternalKey)

            val result = AccountDayCoordinateOverride.resolve(account, day)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("대상 외 거래처 + 수요일 -> null")
        fun resolve_otherAccount_returnsNull() {
            val account = Account(externalKey = "9999999")

            val result = AccountDayCoordinateOverride.resolve(account, DayOfWeek.WEDNESDAY)

            assertThat(result).isNull()
        }

        @Test
        @DisplayName("externalKey 앞뒤 공백 -> trim 후 매칭 성공")
        fun resolve_externalKeyWithWhitespace_matches() {
            val account = Account(externalKey = "  $jmartExternalKey  ")

            val result = AccountDayCoordinateOverride.resolve(account, DayOfWeek.WEDNESDAY)

            assertThat(result).isNotNull
        }

        @Test
        @DisplayName("externalKey=null -> null")
        fun resolve_nullExternalKey_returnsNull() {
            val account = Account(externalKey = null)

            assertThat(AccountDayCoordinateOverride.resolve(account, DayOfWeek.WEDNESDAY)).isNull()
        }

        @Test
        @DisplayName("externalKey=공백 -> null")
        fun resolve_blankExternalKey_returnsNull() {
            val account = Account(externalKey = "   ")

            assertThat(AccountDayCoordinateOverride.resolve(account, DayOfWeek.WEDNESDAY)).isNull()
        }

        @Test
        @DisplayName("account=null -> null")
        fun resolve_nullAccount_returnsNull() {
            assertThat(AccountDayCoordinateOverride.resolve(null, DayOfWeek.WEDNESDAY)).isNull()
        }
    }
}
