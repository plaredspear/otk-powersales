package com.otoki.powersales.user.service

import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.repository.dto.OrganizationCacheDto
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("UserRoleBackfillRunner 테스트")
class UserRoleBackfillRunnerTest {

    private val userRepository: UserRepository = mockk(relaxed = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)
    private val synchronizer: UserOrgDisplayFieldsSynchronizer = mockk(relaxed = true)
    private val resolver: UserRoleAssignmentResolver = mockk(relaxed = true)
    private val adminDataScopeCache: AdminDataScopeCache = mockk(relaxed = true)
    private val transactionTemplate: TransactionTemplate = mockk()

    private val runner = UserRoleBackfillRunner(
        userRepository,
        employeeRepository,
        synchronizer,
        resolver,
        adminDataScopeCache,
        transactionTemplate,
    )

    init {
        every { transactionTemplate.execute(any<TransactionCallback<*>>()) } answers {
            firstArg<TransactionCallback<*>>().doInTransaction(mockk(relaxed = true))
        }
    }

    private val org = OrganizationCacheDto(
        orgCodeLevel3 = "3000",
        orgNameLevel3 = "Retail사업부",
        orgNameLevel4 = "3영업부",
        costCenterLevel3 = "3000",
        orgCodeLevel5 = "5826",
    )

    private fun run() = runner.run(DefaultApplicationArguments())

    private fun user(code: String, roleId: Long? = null) =
        User(username = "$code@otoki.com", employeeCode = code, password = "x")
            .apply { userRoleId = roleId }

    private fun employee(code: String) = Employee(employeeCode = code, name = "사원$code")

    private fun stubOne(code: String, user: User, roleId: Long?) {
        every { userRepository.findEmployeeCodesWithoutUserRole() } returns listOf(code)
        every { resolver.loadNameIndex() } returns mapOf("인천1지점_조장" to 52L)
        every { userRepository.findByEmployeeCodeIn(listOf(code)) } returns listOf(user)
        every { employeeRepository.findByEmployeeCodeIn(listOf(code)) } returns listOf(employee(code))
        every { synchronizer.resolveOrg(any()) } returns org
        every { resolver.resolveUserRoleId(any(), any(), any()) } returns roleId
    }

    @Test
    @DisplayName("대상 없음 - 색인 적재도 트랜잭션도 열지 않는다")
    fun noTargets() {
        every { userRepository.findEmployeeCodesWithoutUserRole() } returns emptyList()

        run()

        verify(exactly = 0) { resolver.loadNameIndex() }
        verify(exactly = 0) { transactionTemplate.execute(any<TransactionCallback<*>>()) }
    }

    @Test
    @DisplayName("정상 - UserRole 배정 + 데이터 스코프 캐시 무효화")
    fun assignsRole() {
        val u = user("20250156")
        stubOne("20250156", u, 52L)

        run()

        assertThat(u.userRoleId).isEqualTo(52L)
        verify { adminDataScopeCache.invalidate(u.id) }
    }

    @Test
    @DisplayName("이름 미매칭 - 기존 값 유지, 캐시 무효화도 하지 않음 (레거시 이탈)")
    fun keepsExistingWhenUnresolved() {
        val u = user("20250156")
        stubOne("20250156", u, null)

        run()

        assertThat(u.userRoleId).isNull()
        verify(exactly = 0) { adminDataScopeCache.invalidate(any()) }
    }

    @Test
    @DisplayName("이미 배정된 행 - 덮어쓰지 않는다 (동시 부팅 인스턴스 방어)")
    fun skipsAlreadyAssigned() {
        val u = user("20250156", roleId = 99L)
        stubOne("20250156", u, 52L)

        run()

        assertThat(u.userRoleId).isEqualTo(99L)
        verify(exactly = 0) { resolver.resolveUserRoleId(any(), any(), any()) }
    }

    @Test
    @DisplayName("조직 미매칭 - 배정하지 않는다 (SF orgInfoTmp == null 가드)")
    fun skipsWhenOrgMissing() {
        val u = user("20250156")
        stubOne("20250156", u, 52L)
        every { synchronizer.resolveOrg(any()) } returns null

        run()

        assertThat(u.userRoleId).isNull()
    }

    @Test
    @DisplayName("사원 미매칭 - 배정 호출 없음")
    fun skipsWhenEmployeeMissing() {
        stubOne("20250156", user("20250156"), 52L)
        every { employeeRepository.findByEmployeeCodeIn(listOf("20250156")) } returns emptyList()

        run()

        verify(exactly = 0) { resolver.resolveUserRoleId(any(), any(), any()) }
    }

    @Test
    @DisplayName("UserRole 마스터가 비면 catch-up 중단")
    fun abortsOnEmptyRoleMaster() {
        every { userRepository.findEmployeeCodesWithoutUserRole() } returns listOf("20250156")
        every { resolver.loadNameIndex() } returns emptyMap()

        run()

        verify(exactly = 0) { transactionTemplate.execute(any<TransactionCallback<*>>()) }
    }

    @Test
    @DisplayName("대상 조회 실패 - 부팅을 막지 않는다")
    fun swallowsLookupFailure() {
        every { userRepository.findEmployeeCodesWithoutUserRole() } throws IllegalStateException("boom")

        assertThatCode { run() }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("청크 실패 - 예외를 삼키고 다음 청크를 계속 처리")
    fun continuesAfterChunkFailure() {
        val codes = (1..600).map { "E$it" }
        every { userRepository.findEmployeeCodesWithoutUserRole() } returns codes
        every { resolver.loadNameIndex() } returns mapOf("x" to 1L)
        every { userRepository.findByEmployeeCodeIn(codes.take(500)) } throws IllegalStateException("boom")
        every { userRepository.findByEmployeeCodeIn(codes.drop(500)) } returns listOf(user("E501"))
        every { employeeRepository.findByEmployeeCodeIn(codes.drop(500)) } returns listOf(employee("E501"))
        every { synchronizer.resolveOrg(any()) } returns org
        every { resolver.resolveUserRoleId(any(), any(), any()) } returns 52L

        assertThatCode { run() }.doesNotThrowAnyException()

        verify(exactly = 2) { transactionTemplate.execute(any<TransactionCallback<*>>()) }
        verify(exactly = 1) { resolver.resolveUserRoleId(any(), any(), any()) }
    }
}
