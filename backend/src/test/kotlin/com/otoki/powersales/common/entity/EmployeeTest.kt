package com.otoki.powersales.common.entity

import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.auth.entity.UserRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * User Entity 테스트
 */
@DisplayName("User Entity 테스트")
class EmployeeTest {

    private fun createTestEmployee(
        employeeCode: String = "12345678",
        password: String = "encodedPassword",
        name: String = "홍길동",
        orgName: String = "서울지점",
        role: UserRole? = null,
        passwordChangeRequired: Boolean = true,
        agreementFlag: Boolean? = null
    ): Employee {
        return Employee(
            employeeCode = employeeCode,
            password = password,
            name = name,
            orgName = orgName,
            role = role,
            passwordChangeRequired = passwordChangeRequired,
            agreementFlag = agreementFlag
        )
    }

    @Test
    @DisplayName("비밀번호 변경 시 새로운 비밀번호가 설정된다")
    fun changePassword_SetsNewPassword() {
        // given
        val employee = createTestEmployee(password = "oldPassword")
        val newPassword = "newEncodedPassword"

        // when
        employee.changePassword(newPassword)

        // then
        assertThat(employee.password).isEqualTo(newPassword)
    }

    @Test
    @DisplayName("비밀번호 변경 시 passwordChangeRequired가 false로 설정된다")
    fun changePassword_SetsPasswordChangeRequiredToFalse() {
        // given
        val employee = createTestEmployee(passwordChangeRequired = true)
        val newPassword = "newEncodedPassword"

        // when
        employee.changePassword(newPassword)

        // then
        assertThat(employee.passwordChangeRequired).isFalse()
    }

    @Test
    @DisplayName("비밀번호 변경 시 updatedAt가 갱신된다")
    fun changePassword_UpdatesUpdDate() {
        // given
        val employee = createTestEmployee()
        val originalUpdatedAt = employee.updatedAt

        // when
        employee.changePassword("newEncodedPassword")

        // then
        assertThat(employee.updatedAt).isAfterOrEqualTo(originalUpdatedAt)
    }

    @Test
    @DisplayName("GPS 동의 플래그가 null이면 동의가 필요하다")
    fun requiresGpsConsent_ReturnsTrueWhenAgreementFlagIsNull() {
        // given
        val employee = createTestEmployee(agreementFlag = null)

        // when
        val result = employee.requiresGpsConsent()

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 플래그가 false이면 동의가 필요하다")
    fun requiresGpsConsent_ReturnsTrueWhenAgreementFlagIsFalse() {
        // given
        val employee = createTestEmployee(agreementFlag = false)

        // when
        val result = employee.requiresGpsConsent()

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 플래그가 true이면 동의가 필요하지 않다")
    fun requiresGpsConsent_ReturnsFalseWhenAgreementFlagIsTrue() {
        // given
        val employee = createTestEmployee(agreementFlag = true)

        // when
        val result = employee.requiresGpsConsent()

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("GPS 동의 기록 시 agreementFlag가 true로 설정된다")
    fun recordGpsConsent_SetsAgreementFlagToTrue() {
        // given
        val employee = createTestEmployee(agreementFlag = null)

        // when
        employee.recordGpsConsent()

        // then
        assertThat(employee.agreementFlag).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 기록 시 updatedAt가 갱신된다")
    fun recordGpsConsent_UpdatesUpdDate() {
        // given
        val employee = createTestEmployee()

        // when
        employee.recordGpsConsent()

        // then
        assertThat(employee.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("사용자 생성 시 기본값이 올바르게 설정된다")
    fun createUser_SetsDefaultValuesCorrectly() {
        // given & when
        val employee = createTestEmployee()

        // then
        assertThat(employee.id).isEqualTo(0)
        assertThat(employee.role).isNull()
        assertThat(employee.passwordChangeRequired).isTrue()
        assertThat(employee.agreementFlag).isNull()
    }

    @Test
    @DisplayName("role 필드가 입력값 그대로 저장된다 (Spec #573 — computed property 제거)")
    fun role_StoredAsIs() {
        // given & when
        val nullRole = createTestEmployee(role = null)
        val woman = createTestEmployee(role = UserRole.WOMAN)
        val leader = createTestEmployee(role = UserRole.LEADER)
        val branchManager = createTestEmployee(role = UserRole.BRANCH_MANAGER)
        val unknown = createTestEmployee(role = UserRole.UNKNOWN)

        // then
        assertThat(nullRole.role).isNull()
        assertThat(woman.role).isEqualTo(UserRole.WOMAN)
        assertThat(leader.role).isEqualTo(UserRole.LEADER)
        assertThat(branchManager.role).isEqualTo(UserRole.BRANCH_MANAGER)
        assertThat(unknown.role).isEqualTo(UserRole.UNKNOWN)
    }
}
