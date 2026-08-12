package com.otoki.powersales.domain.activity.schedule.policy

import com.otoki.powersales.domain.foundation.account.entity.Account
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.DayOfWeek

@DisplayName("AccountDayCoordinateOverrideStore 테스트 — 이동매장 요일별 좌표 예외")
class AccountDayCoordinateOverrideTest {

    private val jmartExternalKey = AccountDayCoordinateOverride.TARGET_EXTERNAL_KEY
    private val coordinateKey = "account_day_coordinate:$jmartExternalKey"
    private val default = AccountDayCoordinateOverride.DEFAULT_COORDINATE

    /** Redis 미주입 store — 코드 기본값 폴백 경로. */
    private val storeWithoutRedis = AccountDayCoordinateOverrideStore(redisTemplate = null)

    private fun storeWith(stored: String?): Pair<AccountDayCoordinateOverrideStore, RedisTemplate<String, String>> {
        val template = mockk<RedisTemplate<String, String>>()
        val ops = mockk<ValueOperations<String, String>>()
        every { template.opsForValue() } returns ops
        every { ops.get(coordinateKey) } returns stored
        every { template.hasKey(coordinateKey) } returns (stored != null)
        return AccountDayCoordinateOverrideStore(template) to template
    }

    @Nested
    @DisplayName("resolve - Redis 미설정 (코드 기본값 폴백)")
    inner class ResolveWithoutRedisTests {

        @Test
        @DisplayName("제이마트 + 수요일 -> 코드 기본값 좌표 반환")
        fun resolve_jmartWednesday_returnsDefaultCoordinate() {
            val account = Account(externalKey = jmartExternalKey)

            val result = storeWithoutRedis.resolve(account, DayOfWeek.WEDNESDAY)

            assertThat(result).isNotNull
            assertThat(result!!.latitude).isCloseTo(38.1018113, within(0.0000001))
            assertThat(result.longitude).isCloseTo(127.9886619, within(0.0000001))
            assertThat(result.label).isEqualTo("제이마트 양구점")
        }

        @ParameterizedTest
        @EnumSource(value = DayOfWeek::class, names = ["WEDNESDAY"], mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("제이마트 + 수요일 외 요일 -> null (거래처 원본 좌표 사용)")
        fun resolve_jmartNonWednesday_returnsNull(day: DayOfWeek) {
            val account = Account(externalKey = jmartExternalKey)

            assertThat(storeWithoutRedis.resolve(account, day)).isNull()
        }

        @Test
        @DisplayName("대상 외 거래처 + 수요일 -> null")
        fun resolve_otherAccount_returnsNull() {
            val account = Account(externalKey = "9999999")

            assertThat(storeWithoutRedis.resolve(account, DayOfWeek.WEDNESDAY)).isNull()
        }

        @Test
        @DisplayName("externalKey 앞뒤 공백 -> trim 후 매칭 성공")
        fun resolve_externalKeyWithWhitespace_matches() {
            val account = Account(externalKey = "  $jmartExternalKey  ")

            assertThat(storeWithoutRedis.resolve(account, DayOfWeek.WEDNESDAY)).isNotNull
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "   "])
        @DisplayName("externalKey 가 빈 문자열/공백 -> null")
        fun resolve_blankExternalKey_returnsNull(externalKey: String) {
            assertThat(storeWithoutRedis.resolve(Account(externalKey = externalKey), DayOfWeek.WEDNESDAY)).isNull()
        }

        @Test
        @DisplayName("externalKey=null / account=null -> null")
        fun resolve_nullInputs_returnsNull() {
            assertThat(storeWithoutRedis.resolve(Account(externalKey = null), DayOfWeek.WEDNESDAY)).isNull()
            assertThat(storeWithoutRedis.resolve(null, DayOfWeek.WEDNESDAY)).isNull()
        }

        @Test
        @DisplayName("Redis 미주입 시 저장/초기화 시도 -> IllegalStateException")
        fun mutate_withoutRedis_throws() {
            assertThatThrownBy { storeWithoutRedis.setCoordinate(default) }
                .isInstanceOf(IllegalStateException::class.java)
            assertThatThrownBy { storeWithoutRedis.reset() }
                .isInstanceOf(IllegalStateException::class.java)
            assertThat(storeWithoutRedis.isCustomized()).isFalse()
        }
    }

    @Nested
    @DisplayName("resolve - Redis 저장값 적용")
    inner class ResolveWithRedisTests {

        @Test
        @DisplayName("저장값(월요일 다른 좌표) -> 월요일에 매칭, 수요일은 미매칭")
        fun resolve_storedMonday_appliesStoredValue() {
            val (store, _) = storeWith("MONDAY|37.5665|126.9780|서울점")
            val account = Account(externalKey = jmartExternalKey)

            val monday = store.resolve(account, DayOfWeek.MONDAY)
            assertThat(monday).isNotNull
            assertThat(monday!!.latitude).isCloseTo(37.5665, within(0.0000001))
            assertThat(monday.longitude).isCloseTo(126.9780, within(0.0000001))
            assertThat(monday.label).isEqualTo("서울점")

            // 코드 기본값 요일(수요일) 은 더 이상 매칭되지 않아야 한다
            assertThat(store.resolve(account, DayOfWeek.WEDNESDAY)).isNull()
        }

        @Test
        @DisplayName("라벨에 구분자가 섞여도 그대로 보존")
        fun deserialize_labelWithDelimiter_preserved() {
            val (store, _) = storeWith("MONDAY|37.5665|126.9780|서울|강남점")

            assertThat(store.getCoordinate().label).isEqualTo("서울|강남점")
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "MONDAY|37.5665|126.9780", // 필드 부족
                "FUNDAY|37.5665|126.9780|x", // 요일 파싱 실패
                "MONDAY|abc|126.9780|x", // 위도 파싱 실패
                "MONDAY|91.0|126.9780|x", // 위도 범위 초과
                "MONDAY|37.5665|181.0|x", // 경도 범위 초과
                "", // 빈 문자열
            ],
        )
        @DisplayName("저장값 형식/범위 오류 -> 코드 기본값 폴백")
        fun getCoordinate_malformedValue_fallsBackToDefault(stored: String) {
            val (store, _) = storeWith(stored)

            assertThat(store.getCoordinate()).isEqualTo(default)
        }

        @Test
        @DisplayName("Redis 조회 예외 -> 코드 기본값 폴백 (예외 전파 안 함)")
        fun getCoordinate_redisFailure_fallsBackToDefault() {
            val template = mockk<RedisTemplate<String, String>>()
            every { template.opsForValue() } throws RuntimeException("redis down")
            val store = AccountDayCoordinateOverrideStore(template)

            assertThat(store.getCoordinate()).isEqualTo(default)
            // 예외 상황에서도 예외 좌표가 살아 있어야 한다 (수요일 등록이 거리 초과로 전면 실패하지 않도록)
            assertThat(store.resolve(Account(externalKey = jmartExternalKey), DayOfWeek.WEDNESDAY)).isNotNull
        }

        @Test
        @DisplayName("키 부재 -> 코드 기본값, customized=false")
        fun getCoordinate_noKey_returnsDefault() {
            val (store, _) = storeWith(null)

            assertThat(store.getCoordinate()).isEqualTo(default)
            assertThat(store.isCustomized()).isFalse()
        }
    }

    @Nested
    @DisplayName("setCoordinate / reset")
    inner class MutateTests {

        @Test
        @DisplayName("기본값과 동일한 값 저장 -> 키 삭제 (기본값=키 부재 규약)")
        fun setCoordinate_sameAsDefault_deletesKey() {
            val template = mockk<RedisTemplate<String, String>>()
            every { template.delete(coordinateKey) } returns true
            val store = AccountDayCoordinateOverrideStore(template)

            store.setCoordinate(default)

            verify(exactly = 1) { template.delete(coordinateKey) }
        }

        @Test
        @DisplayName("기본값과 다른 값 저장 -> 직렬화 문자열 기록")
        fun setCoordinate_customValue_writesSerialized() {
            val template = mockk<RedisTemplate<String, String>>()
            val ops = mockk<ValueOperations<String, String>>()
            every { template.opsForValue() } returns ops
            every { ops.set(coordinateKey, any()) } returns Unit
            val store = AccountDayCoordinateOverrideStore(template)

            store.setCoordinate(
                AccountDayCoordinateOverride.DayCoordinate(DayOfWeek.FRIDAY, 38.121391, 128.208204, "원통점"),
            )

            verify(exactly = 1) { ops.set(coordinateKey, "FRIDAY|38.121391|128.208204|원통점") }
        }

        @Test
        @DisplayName("저장 → 조회 왕복 정합")
        fun setCoordinate_thenGet_roundTrips() {
            val template = mockk<RedisTemplate<String, String>>()
            val ops = mockk<ValueOperations<String, String>>()
            val slot = mutableListOf<String>()
            every { template.opsForValue() } returns ops
            every { ops.set(coordinateKey, capture(slot)) } returns Unit
            every { ops.get(coordinateKey) } answers { slot.lastOrNull() }
            val store = AccountDayCoordinateOverrideStore(template)
            val target = AccountDayCoordinateOverride.DayCoordinate(
                DayOfWeek.SATURDAY, 35.1796, 129.0756, "부산점",
            )

            store.setCoordinate(target)

            assertThat(store.getCoordinate()).isEqualTo(target)
        }

        @Test
        @DisplayName("reset -> 키 삭제")
        fun reset_deletesKey() {
            val template = mockk<RedisTemplate<String, String>>()
            every { template.delete(coordinateKey) } returns true
            val store = AccountDayCoordinateOverrideStore(template)

            store.reset()

            verify(exactly = 1) { template.delete(coordinateKey) }
        }
    }
}
