package com.otoki.internal.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * User Entity 테스트
 */
@DisplayName("User Entity 테스트")
class UserTest {

    private fun createTestUser(
        employeeId: String = "12345678",
        password: String = "encodedPassword",
        name: String = "홍길동",
        department: String = "영업1팀",
        branchName: String = "서울지점",
        role: UserRole = UserRole.USER,
        passwordChangeRequired: Boolean = true,
        lastGpsConsentAt: LocalDateTime? = null
    ): User {
        return User(
            employeeId = employeeId,
            password = password,
            name = name,
            department = department,
            branchName = branchName,
            role = role,
            passwordChangeRequired = passwordChangeRequired,
            lastGpsConsentAt = lastGpsConsentAt
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
    @DisplayName("비밀번호 변경 시 updatedAt이 갱신된다")
    fun changePassword_UpdatesUpdatedAt() {
        // given
        val user = createTestUser()
        val originalUpdatedAt = user.updatedAt
        Thread.sleep(10) // 시간 차이를 보장하기 위한 대기

        // when
        user.changePassword("newEncodedPassword")

        // then
        assertThat(user.updatedAt).isAfter(originalUpdatedAt)
    }

    @Test
    @DisplayName("GPS 동의 이력이 없으면 동의가 필요하다")
    fun requiresGpsConsent_ReturnsTrueWhenLastGpsConsentAtIsNull() {
        // given
        val user = createTestUser(lastGpsConsentAt = null)

        // when
        val result = user.requiresGpsConsent()

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 후 6개월이 경과하면 동의가 필요하다")
    fun requiresGpsConsent_ReturnsTrueWhenSixMonthsElapsed() {
        // given
        val sixMonthsAndOneDayAgo = LocalDateTime.now().minusMonths(6).minusDays(1)
        val user = createTestUser(lastGpsConsentAt = sixMonthsAndOneDayAgo)

        // when
        val result = user.requiresGpsConsent()

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 후 6개월이 경과하지 않았으면 동의가 필요하지 않다")
    fun requiresGpsConsent_ReturnsFalseWhenWithinSixMonths() {
        // given
        val fiveMonthsAgo = LocalDateTime.now().minusMonths(5)
        val user = createTestUser(lastGpsConsentAt = fiveMonthsAgo)

        // when
        val result = user.requiresGpsConsent()

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("GPS 동의를 최근에 했으면 동의가 필요하지 않다")
    fun requiresGpsConsent_ReturnsFalseWhenRecentlyConsented() {
        // given
        val oneDayAgo = LocalDateTime.now().minusDays(1)
        val user = createTestUser(lastGpsConsentAt = oneDayAgo)

        // when
        val result = user.requiresGpsConsent()

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("GPS 동의 기록 시 lastGpsConsentAt이 현재 시간으로 갱신된다")
    fun recordGpsConsent_UpdatesLastGpsConsentAt() {
        // given
        val user = createTestUser(lastGpsConsentAt = null)
        val beforeRecording = LocalDateTime.now().minusSeconds(1)

        // when
        user.recordGpsConsent()

        // then
        assertThat(user.lastGpsConsentAt)
            .isNotNull()
            .isAfter(beforeRecording)
    }

    @Test
    @DisplayName("GPS 동의 기록 시 updatedAt이 갱신된다")
    fun recordGpsConsent_UpdatesUpdatedAt() {
        // given
        val user = createTestUser()
        val originalUpdatedAt = user.updatedAt
        Thread.sleep(10) // 시간 차이를 보장하기 위한 대기

        // when
        user.recordGpsConsent()

        // then
        assertThat(user.updatedAt).isAfter(originalUpdatedAt)
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
        assertThat(user.lastGpsConsentAt).isNull()
        assertThat(user.createdAt).isNotNull()
        assertThat(user.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("다양한 권한으로 사용자를 생성할 수 있다")
    fun createUser_SupportsAllRoles() {
        // given & when
        val regularUser = createTestUser(role = UserRole.USER)
        val leader = createTestUser(role = UserRole.LEADER)
        val admin = createTestUser(role = UserRole.ADMIN)

        // then
        assertThat(regularUser.role).isEqualTo(UserRole.USER)
        assertThat(leader.role).isEqualTo(UserRole.LEADER)
        assertThat(admin.role).isEqualTo(UserRole.ADMIN)
    }
}
