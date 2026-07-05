package com.otoki.powersales.platform.common.config

import com.otoki.powersales.platform.common.exception.FeatureNotYetEnabledException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.mock.env.MockEnvironment

@DisplayName("ProdFeatureGate 테스트")
class ProdFeatureGateTest {

    private fun gate(vararg profiles: String) =
        ProdFeatureGate(MockEnvironment().apply { setActiveProfiles(*profiles) })

    @Test
    @DisplayName("prod 프로파일 - 등록 차단 (FeatureNotYetEnabledException)")
    fun prodBlocksRegistration() {
        val ex = assertThrows<FeatureNotYetEnabledException> {
            gate("prod").assertRegistrationEnabled()
        }
        assertThat(ex.message).isEqualTo("관련 부서 협의 후, 활성화 예정입니다")
        assertThat(ex.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(ex.errorCode).isEqualTo("FEATURE_NOT_YET_ENABLED")
    }

    @Test
    @DisplayName("prod 가 다른 프로파일과 함께 활성이어도 차단")
    fun prodAmongOthersBlocks() {
        assertThrows<FeatureNotYetEnabledException> {
            gate("prod", "someOther").assertRegistrationEnabled()
        }
    }

    @Test
    @DisplayName("dev 프로파일 - 통과")
    fun devPasses() {
        assertThatCode { gate("dev").assertRegistrationEnabled() }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("local 프로파일 - 통과")
    fun localPasses() {
        assertThatCode { gate("local").assertRegistrationEnabled() }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("활성 프로파일 없음 - 통과")
    fun noProfilePasses() {
        assertThatCode { gate().assertRegistrationEnabled() }.doesNotThrowAnyException()
    }
}
