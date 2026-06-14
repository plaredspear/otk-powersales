package com.otoki.powersales.admin.service

import com.otoki.powersales.platform.auth.entity.Profile
import com.otoki.powersales.platform.auth.sharing.repository.SharingPolicyQueryRepository
import com.otoki.powersales.platform.auth.sharing.service.GroupMembershipEvaluator
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.platform.auth.sharing.service.ProfileFlagsEvaluator
import com.otoki.powersales.platform.auth.sharing.service.RecordTypePermissionEvaluator
import com.otoki.powersales.platform.auth.sharing.service.UserRoleHierarchyTraversal
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AdminDataScopeService 테스트")
class AdminDataScopeServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val userRoleHierarchyTraversal: UserRoleHierarchyTraversal = mockk(relaxed = true)
    private val groupMembershipEvaluator: GroupMembershipEvaluator = mockk(relaxed = true)
    private val profileFlagsEvaluator: ProfileFlagsEvaluator = mockk(relaxed = true)
    private val permissionSetEvaluator: PermissionSetEvaluator = mockk(relaxed = true)
    private val sharingPolicyQueryRepository: SharingPolicyQueryRepository = mockk(relaxed = true)
    private val recordTypePermissionEvaluator: RecordTypePermissionEvaluator = mockk(relaxed = true)

    private val adminDataScopeService = AdminDataScopeService(
        employeeRepository = employeeRepository,
        userRepository = userRepository,
        profileRepository = profileRepository,
        userRoleHierarchyTraversal = userRoleHierarchyTraversal,
        groupMembershipEvaluator = groupMembershipEvaluator,
        profileFlagsEvaluator = profileFlagsEvaluator,
        permissionSetEvaluator = permissionSetEvaluator,
        sharingPolicyQueryRepository = sharingPolicyQueryRepository,
        recordTypePermissionEvaluator = recordTypePermissionEvaluator,
    )

    init {
        // relaxed mockk 는 Optional<Profile> / User 의 generic 추론 실패로 Object 반환 → ClassCastException.
        // 명시적 stub default 부여.
        every { profileRepository.findById(any()) } returns Optional.empty<Profile>()
        every { userRepository.findByEmployeeCode(any()) } returns null
    }

    /**
     * service 가 employee.employeeCode → user → profile.name 체인으로 분기.
     * profileName 을 직접 stub 하기 위한 helper.
     */
    private fun stubProfile(employeeCode: String, profileName: String?, isSalesSupport: Boolean = false) {
        val profileId = if (profileName != null) 100L else null
        val user = User(
            username = "user-$employeeCode",
            employeeCode = employeeCode,
            password = "x",
            profileId = profileId,
            isSalesSupport = isSalesSupport,
        )
        every { userRepository.findByEmployeeCode(employeeCode) } returns user
        if (profileName != null) {
            val profile = Profile(id = profileId!!, name = profileName)
            every { profileRepository.findById(profileId) } returns Optional.of(profile)
        }
    }

    @Nested
    @DisplayName("resolve")
    inner class Resolve {

        // ========== 전체 지점 권한 (ALL_BRANCHES_PROFILES + 시스템 관리자 + isSalesSupport) ==========

        @Test
        @DisplayName("Profile=영업부장 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withSalesDirector_returnsAllBranches() {
            val userId = 1L
            val employee = createTestEmployee(id = userId, costCenterCode = "B001")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "3.영업부장")

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("Profile=사업부장 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withDivisionDirector_returnsAllBranches() {
            val userId = 2L
            val employee = createTestEmployee(id = userId, costCenterCode = "B002")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "2.사업부장")

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("Profile=본부장 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withSalesHeadquartersDirector_returnsAllBranches() {
            val userId = 3L
            val employee = createTestEmployee(id = userId, costCenterCode = "B003")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "1.본부장")

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("isSalesSupport=true (영업지원실) -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withSalesSupportOffice_returnsAllBranches() {
            val userId = 4L
            val employee = createTestEmployee(id = userId, costCenterCode = "B004")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "9. Staff", isSalesSupport = true)

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("Profile=시스템 관리자 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withSystemAdmin_returnsAllBranches() {
            val userId = 12L
            val employee = createTestEmployee(id = userId, costCenterCode = "B999")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "시스템 관리자")

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== 지점 한정 (그 외 Profile + costCenterCode) ==========

        @Test
        @DisplayName("일반 Profile + costCenterCode 있음 -> isAllBranches=false, branchCodes 포함")
        fun resolve_withLeaderAndCostCenter_returnsBranchScope() {
            val userId = 5L
            val employee = createTestEmployee(id = userId, costCenterCode = "B100")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "9. Staff")

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B100")
        }

        @Test
        @DisplayName("일반 Profile + 다른 costCenterCode -> isAllBranches=false, branchCodes 포함")
        fun resolve_withBranchManagerAndCostCenter_returnsBranchScope() {
            val userId = 6L
            val employee = createTestEmployee(id = userId, costCenterCode = "B200")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "8. Manager")

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B200")
        }

        @Test
        @DisplayName("일반 Profile + costCenterCode null -> isAllBranches=false, branchCodes 비어있음")
        fun resolve_withLeaderAndNullCostCenter_returnsEmptyBranchCodes() {
            val userId = 7L
            val employee = createTestEmployee(id = userId, costCenterCode = null)
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = "9. Staff")

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== Profile 미지정 ==========

        @Test
        @DisplayName("Profile 미지정 + costCenterCode 있음 -> isAllBranches=false, branchCodes 포함")
        fun resolve_withNullAuthorityAndCostCenter_returnsBranchScope() {
            val userId = 8L
            val employee = createTestEmployee(id = userId, costCenterCode = "B300")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = null)

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B300")
        }

        @Test
        @DisplayName("Profile 미지정 + costCenterCode null -> isAllBranches=false, branchCodes 비어있음")
        fun resolve_withNullAuthorityAndNullCostCenter_returnsEmptyBranchCodes() {
            val userId = 9L
            val employee = createTestEmployee(id = userId, costCenterCode = null)
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            stubProfile(employee.employeeCode!!, profileName = null)

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== User 미존재 (Employee 만 존재) ==========

        @Test
        @DisplayName("User 미존재 + costCenterCode 있음 -> isAllBranches=false, branchCodes 포함")
        fun resolve_withUnknownAuthorityAndCostCenter_returnsEmpty() {
            val userId = 10L
            val employee = createTestEmployee(id = userId, costCenterCode = "B400")
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee
            // User stub 부재 → init 의 default (null) 적용

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B400")
        }

        @Test
        @DisplayName("User 미존재 + costCenterCode null -> isAllBranches=false, branchCodes 비어있음")
        fun resolve_withUnknownAuthorityAndNullCostCenter_returnsEmptyBranchCodes() {
            val userId = 11L
            val employee = createTestEmployee(id = userId, costCenterCode = null)
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns employee

            val result = adminDataScopeService.resolve(userId)

            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== 사용자 미존재 ==========

        @Test
        @DisplayName("존재하지 않는 userId - 조회 실패 -> IllegalStateException 발생")
        fun resolve_withNonExistentUser_throwsException() {
            // Given
            val userId = 999L
            every { employeeRepository.findWithEmployeeInfoById(userId) } returns null

            // When & Then
            assertThatThrownBy { adminDataScopeService.resolve(userId) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("사용자를 찾을 수 없습니다")
                .hasMessageContaining(userId.toString())
        }
    }

    // ========== Helper Methods ==========

    private fun createTestEmployee(
        id: Long = 1L,
        employeeCode: String = "EMP-$id",
        name: String = "홍길동",
        costCenterCode: String? = null
    ): Employee {
        return Employee(
            id = id,
            employeeCode = employeeCode,
            name = name,
            role = null,
            costCenterCode = costCenterCode
        )
    }
}
