package com.otoki.internal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * User Entity 테스트
 */
@DisplayName("User Entity 테스트")
class UserTest {

    private fun createTestUser(
        employeeId: String = "12345678",
        password: String = "encodedPassword",
        name: String = "홍길동",
        orgName: String = "서울지점",
        appAuthority: String? = null,
        passwordChangeRequired: Boolean = true,
        agreementFlag: Boolean? = null
    ): User {
        return User(
            employeeId = employeeId,
            password = password,
            name = name,
            orgName = orgName,
            appAuthority = appAuthority,
            passwordChangeRequired = passwordChangeRequired,
            agreementFlag = agreementFlag
        )
    }

    @Test
    @DisplayName("비밀번호 변경 시 새로운 비밀번호가 설정된다")
    fun changePassword_SetsNewPassword() {
        // given
        val user = createTestUser(password = "oldPassword")
        val newPassword = "newEncodedPassword"

        // when
        user.changePassword(newPassword)

        // then
        assertThat(user.password).isEqualTo(newPassword)
    }

    @Test
    @DisplayName("비밀번호 변경 시 passwordChangeRequired가 false로 설정된다")
    fun changePassword_SetsPasswordChangeRequiredToFalse() {
        // given
        val user = createTestUser(passwordChangeRequired = true)
        val newPassword = "newEncodedPassword"

        // when
        user.changePassword(newPassword)

        // then
        assertThat(user.passwordChangeRequired).isFalse()
    }

    @Test
    @DisplayName("비밀번호 변경 시 updDate가 갱신된다")
    fun changePassword_UpdatesUpdDate() {
        // given
        val user = createTestUser()
        val originalUpdDate = user.updDate

        // when
        user.changePassword("newEncodedPassword")

        // then
        assertThat(user.updDate).isNotNull()
        if (originalUpdDate != null) {
            assertThat(user.updDate).isAfterOrEqualTo(originalUpdDate)
        }
    }

    @Test
    @DisplayName("GPS 동의 플래그가 null이면 동의가 필요하다")
    fun requiresGpsConsent_ReturnsTrueWhenAgreementFlagIsNull() {
        // given
        val user = createTestUser(agreementFlag = null)

        // when
        val result = user.requiresGpsConsent()

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 플래그가 false이면 동의가 필요하다")
    fun requiresGpsConsent_ReturnsTrueWhenAgreementFlagIsFalse() {
        // given
        val user = createTestUser(agreementFlag = false)

        // when
        val result = user.requiresGpsConsent()

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 플래그가 true이면 동의가 필요하지 않다")
    fun requiresGpsConsent_ReturnsFalseWhenAgreementFlagIsTrue() {
        // given
        val user = createTestUser(agreementFlag = true)

        // when
        val result = user.requiresGpsConsent()

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("GPS 동의 기록 시 agreementFlag가 true로 설정된다")
    fun recordGpsConsent_SetsAgreementFlagToTrue() {
        // given
        val user = createTestUser(agreementFlag = null)

        // when
        user.recordGpsConsent()

        // then
        assertThat(user.agreementFlag).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 기록 시 updDate가 갱신된다")
    fun recordGpsConsent_UpdatesUpdDate() {
        // given
        val user = createTestUser()

        // when
        user.recordGpsConsent()

        // then
        assertThat(user.updDate).isNotNull()
    }

    @Test
    @DisplayName("사용자 생성 시 기본값이 올바르게 설정된다")
    fun createUser_SetsDefaultValuesCorrectly() {
        // given & when
        val user = createTestUser()

        // then
        assertThat(user.id).isEqualTo(0)
        assertThat(user.role).isEqualTo(UserRole.USER)
        assertThat(user.passwordChangeRequired).isTrue()
        assertThat(user.agreementFlag).isNull()
    }

    @Test
    @DisplayName("appAuthority로부터 role이 올바르게 도출된다")
    fun role_ComputedFromAppAuthority() {
        // given & when
        val regularUser = createTestUser(appAuthority = null)
        val regularUser2 = createTestUser(appAuthority = "여사원")
        val leader = createTestUser(appAuthority = "조장")
        val admin = createTestUser(appAuthority = "지점장")

        // then
        assertThat(regularUser.role).isEqualTo(UserRole.USER)
        assertThat(regularUser2.role).isEqualTo(UserRole.USER)
        assertThat(leader.role).isEqualTo(UserRole.LEADER)
        assertThat(admin.role).isEqualTo(UserRole.ADMIN)
    }
}
