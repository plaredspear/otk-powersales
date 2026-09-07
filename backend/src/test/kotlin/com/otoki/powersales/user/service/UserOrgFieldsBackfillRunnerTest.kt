package com.otoki.powersales.user.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
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

@DisplayName("UserOrgFieldsBackfillRunner 테스트")
class UserOrgFieldsBackfillRunnerTest {

    private val userRepository: UserRepository = mockk(relaxed = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)
    private val synchronizer: UserOrgDisplayFieldsSynchronizer = mockk(relaxed = true)
    private val transactionTemplate: TransactionTemplate = mockk()

    private val runner = UserOrgFieldsBackfillRunner(
        userRepository,
        employeeRepository,
        synchronizer,
        transactionTemplate,
    )

    init {
        // TransactionTemplate 은 콜백을 그대로 실행 — 트랜잭션 경계는 통합 관심사라 단위 테스트에서 제외.
        every { transactionTemplate.execute(any<TransactionCallback<*>>()) } answers {
            firstArg<TransactionCallback<*>>().doInTransaction(mockk(relaxed = true))
        }
    }

    private fun run() = runner.run(DefaultApplicationArguments())

    private fun user(code: String) = User(username = "$code@otoki.com", employeeCode = code, password = "x")

    private fun employee(code: String) = Employee(employeeCode = code, name = "사원$code")

    @Test
    @DisplayName("대상 없음 - 트랜잭션을 열지 않는다")
    fun noTargets_doesNotOpenTransaction() {
        every { userRepository.findEmployeeCodesWithoutOrgDisplayFields() } returns emptyList()

        run()

        verify(exactly = 0) { transactionTemplate.execute(any<TransactionCallback<*>>()) }
    }

    @Test
    @DisplayName("정상 - 사원 매칭된 User 만 동기화")
    fun syncsMatchedUsers() {
        every { userRepository.findEmployeeCodesWithoutOrgDisplayFields() } returns listOf("1001", "1002")
        every { userRepository.findByEmployeeCodeIn(listOf("1001", "1002")) } returns
            listOf(user("1001"), user("1002"))
        every { employeeRepository.findByEmployeeCodeIn(listOf("1001", "1002")) } returns
            listOf(employee("1001"), employee("1002"))
        every { synchronizer.sync(any(), any()) } returns true

        run()

        verify(exactly = 2) { synchronizer.sync(any(), any()) }
    }

    @Test
    @DisplayName("사원 미매칭 - User 는 있으나 Employee 가 없으면 동기화 호출 안 함")
    fun skipsWhenEmployeeMissing() {
        every { userRepository.findEmployeeCodesWithoutOrgDisplayFields() } returns listOf("1001")
        every { userRepository.findByEmployeeCodeIn(listOf("1001")) } returns listOf(user("1001"))
        every { employeeRepository.findByEmployeeCodeIn(listOf("1001")) } returns emptyList()

        run()

        verify(exactly = 0) { synchronizer.sync(any(), any()) }
    }

    @Test
    @DisplayName("User 미매칭 - Employee 는 있으나 User 가 없으면 동기화 호출 안 함")
    fun skipsWhenUserMissing() {
        every { userRepository.findEmployeeCodesWithoutOrgDisplayFields() } returns listOf("1001")
        every { userRepository.findByEmployeeCodeIn(listOf("1001")) } returns emptyList()
        every { employeeRepository.findByEmployeeCodeIn(listOf("1001")) } returns listOf(employee("1001"))

        run()

        verify(exactly = 0) { synchronizer.sync(any(), any()) }
    }

    @Test
    @DisplayName("대상 조회 실패 - 부팅을 막지 않는다")
    fun swallowsLookupFailure() {
        every { userRepository.findEmployeeCodesWithoutOrgDisplayFields() } throws IllegalStateException("boom")

        assertThatCode { run() }.doesNotThrowAnyException()
        verify(exactly = 0) { transactionTemplate.execute(any<TransactionCallback<*>>()) }
    }

    @Test
    @DisplayName("청크 실패 - 예외를 삼키고 다음 청크를 계속 처리")
    fun continuesAfterChunkFailure() {
        // CHUNK_SIZE(500) 를 넘겨 2청크로 나뉘게 한다.
        val codes = (1..600).map { "E$it" }
        every { userRepository.findEmployeeCodesWithoutOrgDisplayFields() } returns codes
        // 첫 청크만 실패, 두 번째 청크는 정상 처리되어야 한다.
        every { userRepository.findByEmployeeCodeIn(codes.take(500)) } throws IllegalStateException("boom")
        every { userRepository.findByEmployeeCodeIn(codes.drop(500)) } returns listOf(user("E501"))
        every { employeeRepository.findByEmployeeCodeIn(codes.drop(500)) } returns listOf(employee("E501"))
        every { synchronizer.sync(any(), any()) } returns true

        assertThatCode { run() }.doesNotThrowAnyException()

        verify(exactly = 2) { transactionTemplate.execute(any<TransactionCallback<*>>()) }
        verify(exactly = 1) { synchronizer.sync(any(), any()) }
    }

    @Test
    @DisplayName("멱등 - 조직 마스터 미적재(sync=false)여도 예외 없이 종료")
    fun toleratesUnsyncedRows() {
        every { userRepository.findEmployeeCodesWithoutOrgDisplayFields() } returns listOf("1001")
        every { userRepository.findByEmployeeCodeIn(listOf("1001")) } returns listOf(user("1001"))
        every { employeeRepository.findByEmployeeCodeIn(listOf("1001")) } returns listOf(employee("1001"))
        every { synchronizer.sync(any(), any()) } returns false

        assertThatCode { run() }.doesNotThrowAnyException()

        assertThat(true).isTrue()
    }
}
