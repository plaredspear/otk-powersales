package com.otoki.powersales.common.entity

import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.entity.UserRole
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
        appAuthority: String? = null,
        passwordChangeRequired: Boolean = true,
        agreementFlag: Boolean? = null
    ): Employee {
        return Employee(
            employeeCode = employeeCode,
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
        assertThat(employee.role).isEqualTo(UserRole.USER)
        assertThat(employee.passwordChangeRequired).isTrue()
        assertThat(employee.agreementFlag).isNull()
    }

    @Test
    @DisplayName("appAuthority로부터 role이 올바르게 도출된다")
    fun role_ComputedFromAppAuthority() {
        // given & when
        val regularUser = createTestEmployee(appAuthority = null)
        val regularUser2 = createTestEmployee(appAuthority = "여사원")
        val leader = createTestEmployee(appAuthority = "조장")
        val admin = createTestEmployee(appAuthority = "지점장")

        // then
        assertThat(regularUser.role).isEqualTo(UserRole.USER)
        assertThat(regularUser2.role).isEqualTo(UserRole.USER)
        assertThat(leader.role).isEqualTo(UserRole.LEADER)
        assertThat(admin.role).isEqualTo(UserRole.ADMIN)
    }

    @Test
    @DisplayName("매핑 테이블에 없는 appAuthority 값은 USER로 폴백된다")
    fun role_UnknownAppAuthority_FallsBackToUser() {
        // given & when
        val unknownRole = createTestEmployee(appAuthority = "대리")
        val emptyRole = createTestEmployee(appAuthority = "")

        // then
        assertThat(unknownRole.role).isEqualTo(UserRole.USER)
        assertThat(emptyRole.role).isEqualTo(UserRole.USER)
    }
}
