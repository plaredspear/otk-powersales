package com.otoki.powersales.admin.tools.feature

import com.otoki.powersales.admin.tools.feature.service.FeatureToggleService
import com.otoki.powersales.admin.tools.feature.service.FeatureToggleState
import com.otoki.powersales.admin.tools.feature.service.FeatureToggleStore
import com.otoki.powersales.platform.common.exception.BusinessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("FeatureToggleService 테스트")
class FeatureToggleServiceTest {

    private val store: FeatureToggleStore = mockk()
    private val service = FeatureToggleService(store)

    @Test
    @DisplayName("list - 전체 flag 상태를 label 과 함께 반환")
    fun list_returnsAllFlags() {
        every { store.getState(any()) } returns FeatureToggleState(enabled = true, reason = null)
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = "점검 중")

        val result = service.list()

        assertThat(result).hasSize(FeatureFlag.entries.size)
        assertThat(result.map { it.code })
            .containsExactly("PRODUCT_CLAIM", "LOGISTICS_CLAIM", "ORDER_REQUEST")
        val order = result.first { it.code == "ORDER_REQUEST" }
        assertThat(order.label).isEqualTo("주문 등록")
        assertThat(order.enabled).isFalse()
        assertThat(order.reason).isEqualTo("점검 중")
    }

    @Test
    @DisplayName("setEnabled - store 에 위임 후 최신 상태 반환")
    fun setEnabled_delegatesAndReturnsLatest() {
        every { store.setState(FeatureFlag.PRODUCT_CLAIM, false, "잠시 중지") } returns Unit
        every { store.getState(FeatureFlag.PRODUCT_CLAIM) } returns
            FeatureToggleState(enabled = false, reason = "잠시 중지")

        val result = service.setEnabled(FeatureFlag.PRODUCT_CLAIM, false, "잠시 중지")

        assertThat(result.enabled).isFalse()
        assertThat(result.reason).isEqualTo("잠시 중지")
        verify(exactly = 1) { store.setState(FeatureFlag.PRODUCT_CLAIM, false, "잠시 중지") }
    }

    @Test
    @DisplayName("ensureEnabled - 활성이면 통과")
    fun ensureEnabled_passesWhenEnabled() {
        every { store.getState(FeatureFlag.PRODUCT_CLAIM) } returns
            FeatureToggleState(enabled = true, reason = null)

        assertThatCode { service.ensureEnabled(FeatureFlag.PRODUCT_CLAIM) }
            .doesNotThrowAnyException()
    }

    @Test
    @DisplayName("ensureEnabled - 비활성 + 사유면 사유를 메시지로 409")
    fun ensureEnabled_throwsWithReason() {
        every { store.getState(FeatureFlag.LOGISTICS_CLAIM) } returns
            FeatureToggleState(enabled = false, reason = "물류 시스템 점검 중")

        assertThatThrownBy { service.ensureEnabled(FeatureFlag.LOGISTICS_CLAIM) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("물류 시스템 점검 중")
            .extracting { (it as BusinessException).httpStatus }
            .isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @DisplayName("ensureEnabled - 비활성 + 사유 없으면 기본 문구로 409")
    fun ensureEnabled_throwsWithDefaultMessage() {
        every { store.getState(FeatureFlag.ORDER_REQUEST) } returns
            FeatureToggleState(enabled = false, reason = null)

        assertThatThrownBy { service.ensureEnabled(FeatureFlag.ORDER_REQUEST) }
            .isInstanceOf(BusinessException::class.java)
            .hasMessageContaining("주문 등록")
            .hasMessageContaining("중지")
    }
}
