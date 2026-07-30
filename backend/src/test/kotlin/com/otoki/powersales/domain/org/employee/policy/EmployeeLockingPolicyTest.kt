package com.otoki.powersales.domain.org.employee.policy

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.entity.AppAuthority
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("EmployeeLockingPolicy 테스트 (SF EmployeeTrigger before insert/update 동등)")
class EmployeeLockingPolicyTest {

    private fun employee(
        jobCode: String? = null,
        status: String? = "재직",
        role: String? = null,
        lockingFlag: Boolean? = null,
        appLoginActive: Boolean? = null,
    ) = Employee(employeeCode = "100123", name = "테스트").apply {
        this.jobCode = jobCode
        this.status = status
        this.role = role
        this.lockingFlag = lockingFlag
        this.appLoginActive = appLoginActive
    }

    @Nested
    @DisplayName("잠금 ON -> 앱 로그인 OFF (SF cls:40)")
    inner class LockingDisablesLogin {

        @Test
        @DisplayName("보호 대상 아님 + lockingFlag=true -> appLoginActive=false")
        fun locked_disablesLogin() {
            val entity = employee(jobCode = "사무직", lockingFlag = true, appLoginActive = true)

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.lockingFlag).isTrue()
            assertThat(entity.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("lockingFlag=false -> appLoginActive 무변경 (기존 값 보존)")
        fun notLocked_keepsLogin() {
            val entity = employee(jobCode = "사무직", lockingFlag = false, appLoginActive = false)

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("lockingFlag=null -> appLoginActive 무변경")
        fun nullLocking_keepsLogin() {
            val entity = employee(jobCode = "사무직", lockingFlag = null, appLoginActive = true)

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.appLoginActive).isTrue()
        }
    }

    @Nested
    @DisplayName("현장 여사원 직군 보호 (SF cls:45-53 lockingFlagException)")
    inner class ProtectedRestore {

        @Test
        @DisplayName("판촉직 + 여사원 + 재직 + 잠금 시도 -> 잠금 해제 + 앱 로그인 복원")
        fun promotionWoman_restored() {
            val entity = employee(
                jobCode = "판촉직",
                role = AppAuthority.WOMAN,
                lockingFlag = true,
                appLoginActive = false,
            )

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.lockingFlag).isFalse()
            assertThat(entity.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("OSC직 + 조장 + 재직 + 잠금 시도 -> 보호 복원")
        fun oscLeader_restored() {
            val entity = employee(
                jobCode = "OSC직",
                role = AppAuthority.LEADER,
                lockingFlag = true,
                appLoginActive = false,
            )

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.lockingFlag).isFalse()
            assertThat(entity.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("레이디직(구 OSC 명칭) + 여사원 -> 보호 복원")
        fun ladyWoman_restored() {
            val entity = employee(
                jobCode = "레이디직",
                role = AppAuthority.WOMAN,
                lockingFlag = true,
                appLoginActive = false,
            )

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("판촉직 + 여사원 + 퇴직 -> 보호 미적용 (잠금 유지)")
        fun retired_notProtected() {
            val entity = employee(
                jobCode = "판촉직",
                status = "퇴직",
                role = AppAuthority.WOMAN,
                lockingFlag = true,
                appLoginActive = true,
            )

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.lockingFlag).isTrue()
            assertThat(entity.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("판촉직 + 지점장(보호 권한 아님) -> 보호 미적용")
        fun branchManager_notProtected() {
            val entity = employee(
                jobCode = "판촉직",
                role = AppAuthority.BRANCH_MANAGER,
                lockingFlag = true,
                appLoginActive = true,
            )

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.lockingFlag).isTrue()
            assertThat(entity.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("사무직(보호 직군 아님) + 여사원 -> 보호 미적용")
        fun nonFieldJobCode_notProtected() {
            val entity = employee(
                jobCode = "사무직",
                role = AppAuthority.WOMAN,
                lockingFlag = true,
                appLoginActive = true,
            )

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("보호 대상 + 잠금 없음 + 앱 로그인 꺼짐 -> 앱 로그인 복원 (보호는 잠금과 무관)")
        fun protectedWithoutLocking_stillRestores() {
            val entity = employee(
                jobCode = "판촉직",
                role = AppAuthority.WOMAN,
                lockingFlag = false,
                appLoginActive = false,
            )

            EmployeeLockingPolicy.applyBeforeSave(entity)

            assertThat(entity.appLoginActive).isTrue()
        }
    }

    @Test
    @DisplayName("적용 순서 - 보호(2단계)가 잠금(1단계)보다 뒤라 보호 대상은 잠기지 않는다")
    fun protectionWinsOverLocking() {
        val entity = employee(
            jobCode = "판촉직",
            role = AppAuthority.LEADER,
            lockingFlag = true,
            appLoginActive = true,
        )

        EmployeeLockingPolicy.applyBeforeSave(entity)

        assertThat(entity.lockingFlag).isFalse()
        assertThat(entity.appLoginActive).isTrue()
    }
}
