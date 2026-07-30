package com.otoki.powersales.admin.tools.feature

import com.otoki.powersales.admin.tools.feature.service.FeatureToggleStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.SetOperations
import org.springframework.data.redis.core.ValueOperations

@DisplayName("FeatureToggleStore 테스트")
class FeatureToggleStoreTest {

    private val redisTemplate: RedisTemplate<String, String> = mockk()
    private val valueOps: ValueOperations<String, String> = mockk()
    private val setOps: SetOperations<String, String> = mockk()
    private val store = FeatureToggleStore(redisTemplate)

    private fun stubValueOps() {
        every { redisTemplate.opsForValue() } returns valueOps
    }

    @Test
    @DisplayName("getState - disabled 키 부재면 활성(사유 없음)")
    fun getState_absentKeyMeansEnabled() {
        stubValueOps()
        every { valueOps.get("feature_toggle:disabled:PRODUCT_CLAIM") } returns null

        val state = store.getState(FeatureFlag.PRODUCT_CLAIM)

        assertThat(state.enabled).isTrue()
        assertThat(state.reason).isNull()
    }

    @Test
    @DisplayName("getState - disabled 키 존재면 비활성 + 사유 반환")
    fun getState_disabledKeyReturnsReason() {
        stubValueOps()
        every { valueOps.get("feature_toggle:disabled:ORDER_REQUEST") } returns "1"
        every { valueOps.get("feature_toggle:reason:ORDER_REQUEST") } returns "점검 중"

        val state = store.getState(FeatureFlag.ORDER_REQUEST)

        assertThat(state.enabled).isFalse()
        assertThat(state.reason).isEqualTo("점검 중")
    }

    @Test
    @DisplayName("getState - Redis 미가동(null template)이면 활성 폴백")
    fun getState_nullTemplateFallsBackToEnabled() {
        val storeNoRedis = FeatureToggleStore(null)

        val state = storeNoRedis.getState(FeatureFlag.LOGISTICS_CLAIM)

        assertThat(state.enabled).isTrue()
        assertThat(state.reason).isNull()
    }

    @Test
    @DisplayName("getState - Redis 조회 예외면 활성 폴백")
    fun getState_redisExceptionFallsBackToEnabled() {
        stubValueOps()
        every { valueOps.get(any()) } throws RuntimeException("connection refused")

        val state = store.getState(FeatureFlag.PRODUCT_CLAIM)

        assertThat(state.enabled).isTrue()
        assertThat(state.reason).isNull()
    }

    @Test
    @DisplayName("setState - 활성화면 disabled/reason 키 모두 제거")
    fun setState_enableDeletesKeys() {
        every { redisTemplate.delete(any<Collection<String>>()) } returns 2L

        store.setState(FeatureFlag.PRODUCT_CLAIM, enabled = true, reason = null)

        verify {
            redisTemplate.delete(
                listOf(
                    "feature_toggle:disabled:PRODUCT_CLAIM",
                    "feature_toggle:reason:PRODUCT_CLAIM",
                ),
            )
        }
    }

    @Test
    @DisplayName("setState - 비활성화 + 사유면 disabled/reason 키 세팅")
    fun setState_disableWithReasonSetsKeys() {
        stubValueOps()
        every { valueOps.set(any(), any()) } returns Unit

        store.setState(FeatureFlag.ORDER_REQUEST, enabled = false, reason = "  점검 중  ")

        verify { valueOps.set("feature_toggle:disabled:ORDER_REQUEST", "1") }
        verify { valueOps.set("feature_toggle:reason:ORDER_REQUEST", "점검 중") }
    }

    @Test
    @DisplayName("setState - 비활성화 + 빈 사유면 reason 키 제거")
    fun setState_disableWithBlankReasonDeletesReasonKey() {
        stubValueOps()
        every { valueOps.set(any(), any()) } returns Unit
        every { redisTemplate.delete(any<String>()) } returns true

        store.setState(FeatureFlag.LOGISTICS_CLAIM, enabled = false, reason = "   ")

        verify { valueOps.set("feature_toggle:disabled:LOGISTICS_CLAIM", "1") }
        verify { redisTemplate.delete("feature_toggle:reason:LOGISTICS_CLAIM") }
    }

    @Test
    @DisplayName("setState - Redis 미가동(null template)이면 예외")
    fun setState_nullTemplateThrows() {
        val storeNoRedis = FeatureToggleStore(null)

        org.assertj.core.api.Assertions
            .assertThatThrownBy { storeNoRedis.setState(FeatureFlag.PRODUCT_CLAIM, false, "x") }
            .isInstanceOf(IllegalStateException::class.java)
    }

    private fun stubSetOps() {
        every { redisTemplate.opsForSet() } returns setOps
    }

    @Test
    @DisplayName("getExemptEmployeeCodes - Set 멤버를 사번 오름차순으로 반환")
    fun getExemptEmployeeCodes_returnsSorted() {
        stubSetOps()
        every { setOps.members("feature_toggle:exempt:ORDER_REQUEST") } returns
            setOf("22222222", "11111111")

        assertThat(store.getExemptEmployeeCodes(FeatureFlag.ORDER_REQUEST))
            .containsExactly("11111111", "22222222")
    }

    @Test
    @DisplayName("getExemptEmployeeCodes - Redis 장애면 빈 목록 폴백")
    fun getExemptEmployeeCodes_redisExceptionFallsBackToEmpty() {
        stubSetOps()
        every { setOps.members(any()) } throws RuntimeException("connection refused")

        assertThat(store.getExemptEmployeeCodes(FeatureFlag.ORDER_REQUEST)).isEmpty()
    }

    @Test
    @DisplayName("isExemptEmployee - Set 멤버면 true, 아니면 false")
    fun isExemptEmployee_checksMembership() {
        stubSetOps()
        every { setOps.isMember("feature_toggle:exempt:ORDER_REQUEST", "11111111") } returns true
        every { setOps.isMember("feature_toggle:exempt:ORDER_REQUEST", "22222222") } returns false

        assertThat(store.isExemptEmployee(FeatureFlag.ORDER_REQUEST, "11111111")).isTrue()
        assertThat(store.isExemptEmployee(FeatureFlag.ORDER_REQUEST, "22222222")).isFalse()
    }

    @Test
    @DisplayName("isExemptEmployee - Redis 미가동(null template)이면 false")
    fun isExemptEmployee_nullTemplateReturnsFalse() {
        val storeNoRedis = FeatureToggleStore(null)

        assertThat(storeNoRedis.isExemptEmployee(FeatureFlag.ORDER_REQUEST, "11111111")).isFalse()
    }

    @Test
    @DisplayName("addExemptEmployee / removeExemptEmployee - flag 별 exempt Set 을 갱신")
    fun addRemoveExemptEmployee_updatesSet() {
        stubSetOps()
        every { setOps.add("feature_toggle:exempt:ORDER_REQUEST", "11111111") } returns 1L
        every { setOps.remove("feature_toggle:exempt:PRODUCT_CLAIM", "11111111") } returns 1L

        store.addExemptEmployee(FeatureFlag.ORDER_REQUEST, "11111111")
        store.removeExemptEmployee(FeatureFlag.PRODUCT_CLAIM, "11111111")

        verify { setOps.add("feature_toggle:exempt:ORDER_REQUEST", "11111111") }
        verify { setOps.remove("feature_toggle:exempt:PRODUCT_CLAIM", "11111111") }
    }

    @Test
    @DisplayName("setState - 활성/비활성 전환은 exempt Set 을 건드리지 않음")
    fun setState_keepsExemptSet() {
        stubValueOps()
        every { valueOps.set(any(), any()) } returns Unit
        every { redisTemplate.delete(any<Collection<String>>()) } returns 2L

        store.setState(FeatureFlag.ORDER_REQUEST, enabled = false, reason = "점검 중")
        store.setState(FeatureFlag.ORDER_REQUEST, enabled = true, reason = null)

        verify(exactly = 0) { redisTemplate.opsForSet() }
        verify(exactly = 0) { redisTemplate.delete("feature_toggle:exempt:ORDER_REQUEST") }
    }
}
