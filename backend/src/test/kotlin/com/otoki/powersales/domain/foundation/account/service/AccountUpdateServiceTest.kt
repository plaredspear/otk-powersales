package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.exception.AccountNameBlankException
import com.otoki.powersales.domain.foundation.account.exception.AccountNameDuplicateException
import com.otoki.powersales.domain.foundation.account.exception.AccountNamePrefixRequiredForUpdateException
import com.otoki.powersales.domain.foundation.account.exception.AccountNotFoundException
import com.otoki.powersales.domain.foundation.account.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("AccountUpdateService 테스트 (Spec #643 P1-B)")
class AccountUpdateServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val service = AccountUpdateService(
        accountRepository,
        employeeRepository,
    )

    private val accountId = 1234L
    private fun webPrincipal(userId: Long, role: String?, profileName: String = "9. Staff") = WebUserPrincipal(
        userId = userId,
        usernameValue = "u$userId@otokims.co.kr",
        employeeCode = "S$userId",
        employeeId = userId,
        role = role,
        costCenterCode = null,
        profileName = profileName,
        isSalesSupport = false,
        passwordChangeRequired = false,
        permissions = emptySet(),
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true
    )

    private val branchManagerPrincipal = webPrincipal(1L, AppAuthority.BRANCH_MANAGER)
    private val systemAdminPrincipal = webPrincipal(9L, null, profileName = "시스템 관리자")

    private fun nativeAccount(
        id: Long = accountId,
        name: String? = "(신규) 강남점",
        employeeCode: String? = "100123",
        branchCode: String? = "C001",
        branchName: String? = "강남지점",
        address1: String? = null,
        phone: String? = null,
        externalKey: String? = null,
        isDeleted: Boolean? = null
    ): Account = Account(
        id = id,
        name = name,
        employeeCode = employeeCode,
        branchCode = branchCode,
        branchName = branchName,
        address1 = address1,
        phone = phone,
        externalKey = externalKey,
        accountGroup = "9999",
        isDeleted = isDeleted
    )

    @BeforeEach
    fun setUp() {
        every { accountRepository.findActiveById(accountId) } returns nativeAccount()
        every { accountRepository.existsActiveByNameAndIdNot(any(), any()) } returns false
    }

    @Nested
    @DisplayName("정상 수정 / PUT 부분 갱신 시맨틱")
    inner class HappyPathAndPartialUpdate {

        @Test
        @DisplayName("T1 정상 수정 - name + address1 + phone 동시 갱신")
        fun t1_success() {
            val account = nativeAccount(address1 = "기존 주소", phone = "02-0000-0000")
            every { accountRepository.findActiveById(accountId) } returns account

            val response = service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(
                    name = "(신규) 강남점 신호 수정",
                    address1 = "서울특별시 강남구 테헤란로 100",
                    phone = "02-1234-5678"
                )
            )

            assertThat(response.id).isEqualTo(accountId)
            assertThat(response.name).isEqualTo("(신규) 강남점 신호 수정")
            assertThat(response.address1).isEqualTo("서울특별시 강남구 테헤란로 100")
            assertThat(response.phone).isEqualTo("02-1234-5678")
            assertThat(account.name).isEqualTo("(신규) 강남점 신호 수정")
            assertThat(account.address1).isEqualTo("서울특별시 강남구 테헤란로 100")
        }

        @Test
        @DisplayName("T2 빈 페이로드 PUT (모든 필드 null) - 변경 0건 + 200 OK")
        fun t2_emptyPayload() {
            val account = nativeAccount()
            every { accountRepository.findActiveById(accountId) } returns account
            val originalName = account.name
            val originalEmployeeCode = account.employeeCode

            val response = service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest()
            )

            assertThat(response.id).isEqualTo(accountId)
            assertThat(account.name).isEqualTo(originalName)
            assertThat(account.employeeCode).isEqualTo(originalEmployeeCode)
            verify(exactly = 0) { accountRepository.existsActiveByNameAndIdNot(any(), any()) }
        }

        @Test
        @DisplayName("T3 name 만 포함 - name 만 갱신 + 다른 필드 보존")
        fun t3_nameOnly() {
            val account = nativeAccount(address1 = "기존 주소")
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 강남점 신호 수정")
            )

            assertThat(account.name).isEqualTo("(신규) 강남점 신호 수정")
            assertThat(account.address1).isEqualTo("기존 주소")
        }

        @Test
        @DisplayName("T4 name 미포함 PUT - prefix 검증 skip 확인 (다른 필드만 변경)")
        fun t4_nameOmitted_prefixSkipped() {
            val account = nativeAccount()
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(phone = "02-9999-9999")
            )

            assertThat(account.phone).isEqualTo("02-9999-9999")
            verify(exactly = 0) { accountRepository.existsActiveByNameAndIdNot(any(), any()) }
        }

        @Test
        @DisplayName("T12 name 페이로드 미포함 + 기존 entity 의 name 이 prefix 위반 - 검증 skip + 다른 필드 갱신")
        fun t12_existingPrefixViolation_skipWhenNameOmitted() {
            val account = nativeAccount(name = "강남점")
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(phone = "02-9999-9999")
            )

            assertThat(account.name).isEqualTo("강남점")
            assertThat(account.phone).isEqualTo("02-9999-9999")
        }
    }

    @Nested
    @DisplayName("Account 존재 검증")
    inner class AccountExistence {

        @Test
        @DisplayName("T5 비존재 id - ACCOUNT_NOT_FOUND + 메시지에 id 포함")
        fun t5_notFound() {
            every { accountRepository.findActiveById(9999) } returns null

            assertThatThrownBy {
                service.update(
                    id = 9999,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(name = "(신규) 무효")
                )
            }
                .isInstanceOf(AccountNotFoundException::class.java)
                .hasMessage("거래처를 찾을 수 없습니다: 9999")
        }

        @Test
        @DisplayName("T6 is_deleted=true Account - findActiveById 가 null 반환 → ACCOUNT_NOT_FOUND")
        fun t6_softDeletedTreatedAsNotFound() {
            every { accountRepository.findActiveById(accountId) } returns null

            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(name = "(신규) 강남점")
                )
            }.isInstanceOf(AccountNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("name 검증 (페이로드 포함 시)")
    inner class NameValidation {

        @Test
        @DisplayName("T7 name blank - ACCOUNT_NAME_BLANK")
        fun t7_nameBlank() {
            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(name = "   ")
                )
            }.isInstanceOf(AccountNameBlankException::class.java)
        }

        @Test
        @DisplayName("T8 name prefix 미포함 - ACCOUNT_NAME_PREFIX_REQUIRED + 메시지 '거래처 수정은 ((신규)/(기타))...'")
        fun t8_prefixMissing() {
            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(name = "강남점")
                )
            }
                .isInstanceOf(AccountNamePrefixRequiredForUpdateException::class.java)
                .hasMessage("거래처 수정은 ((신규)/(기타)) 중 1개를 필수로 입력하셔야 합니다.")
        }

        @Test
        @DisplayName("T9 name 변경 + 동일명 활성 거래처 존재 - ACCOUNT_NAME_DUPLICATE")
        fun t9_duplicate() {
            every { accountRepository.existsActiveByNameAndIdNot("(신규) 다른지점", accountId) } returns true

            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(name = "(신규) 다른지점")
                )
            }.isInstanceOf(AccountNameDuplicateException::class.java)
        }

        @Test
        @DisplayName("T10 name 변경 안 함 (자기 자신과 동일) - 중복 검증 skip + 성공")
        fun t10_unchangedName_skipDuplicateCheck() {
            val account = nativeAccount(name = "(신규) 강남점")
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 강남점")
            )

            assertThat(account.name).isEqualTo("(신규) 강남점")
            verify(exactly = 0) { accountRepository.existsActiveByNameAndIdNot(any(), any()) }
        }

        @Test
        @DisplayName("T11 name 동일 (변경 안 함) but prefix 위반 - 페이로드에 name 포함 시 항상 발동")
        fun t11_unchangedButPrefixViolation_alwaysFires() {
            val account = nativeAccount(name = "강남점")
            every { accountRepository.findActiveById(accountId) } returns account

            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(name = "강남점")
                )
            }.isInstanceOf(AccountNamePrefixRequiredForUpdateException::class.java)
        }
    }

    @Nested
    @DisplayName("employeeCode 검증")
    inner class EmployeeValidation {

        @Test
        @DisplayName("T13 employeeCode 변경 + 신규 Employee 존재 - branch_code/branch_name 보존")
        fun t13_employeeChanged_branchPreserved() {
            val account = nativeAccount(employeeCode = "100123", branchCode = "C001", branchName = "강남지점")
            every { accountRepository.findActiveById(accountId) } returns account
            every { employeeRepository.findByEmployeeCode("200456") } returns
                Optional.of(Employee(id = 20, employeeCode = "200456", name = "이몽룡", costCenterCode = "CC200"))

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(employeeCode = "200456")
            )

            assertThat(account.employeeCode).isEqualTo("200456")
            assertThat(account.branchCode).isEqualTo("C001")
            assertThat(account.branchName).isEqualTo("강남지점")
        }

        @Test
        @DisplayName("T14 employeeCode 변경 + 신규 Employee 미등록 - EMPLOYEE_NOT_FOUND")
        fun t14_employeeNotFound() {
            every { employeeRepository.findByEmployeeCode("99999") } returns Optional.empty()

            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(employeeCode = "99999")
                )
            }.isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("T14-soft Employee 가 is_deleted=true 인 경우도 EMPLOYEE_NOT_FOUND")
        fun t14_softDeletedTreatedAsNotFound() {
            every { employeeRepository.findByEmployeeCode("100200") } returns
                Optional.of(Employee(id = 30, employeeCode = "100200", name = "삭제됨", costCenterCode = "CC300", isDeleted = true))

            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = branchManagerPrincipal,
                    request = AdminAccountUpdateRequest(employeeCode = "100200")
                )
            }.isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("T15 employeeCode 빈 문자열 - null 동등 (변경 안 함)")
        fun t15_employeeCodeEmptyString_equalsNull() {
            val account = nativeAccount(employeeCode = "100123")
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(employeeCode = "")
            )

            assertThat(account.employeeCode).isEqualTo("100123")
            verify(exactly = 0) { employeeRepository.findByEmployeeCode(any()) }
        }
    }

    @Nested
    @DisplayName("SAP 동기 키 silent ignore (Q-C)")
    inner class SapSyncKeyIgnore {

        @Test
        @DisplayName("T16 DTO 정의 자체에 SAP 동기 키 부재 - 페이로드 포함해도 entity 변경 0건")
        fun t16_silentIgnore() {
            val account = nativeAccount(externalKey = "SAP-OLD")
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 변경")
            )

            assertThat(account.externalKey).isEqualTo("SAP-OLD")
        }
    }

    @Nested
    @DisplayName("SYSTEM_ADMIN 검증 우회 (Q-A)")
    inner class SystemAdminBypass {

        @Test
        @DisplayName("T17 SYSTEM_ADMIN + name prefix 위반 - 우회 + 갱신 성공")
        fun t17_systemAdminBypassesPrefix() {
            val account = nativeAccount()
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = systemAdminPrincipal,
                request = AdminAccountUpdateRequest(name = "강남점")
            )

            assertThat(account.name).isEqualTo("강남점")
        }

        @Test
        @DisplayName("T18 SYSTEM_ADMIN + name 동일명 중복 - 우회 + 갱신 성공")
        fun t18_systemAdminBypassesDuplicate() {
            val account = nativeAccount()
            every { accountRepository.findActiveById(accountId) } returns account

            service.update(
                id = accountId,
                principal = systemAdminPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 다른지점")
            )

            assertThat(account.name).isEqualTo("(신규) 다른지점")
            verify(exactly = 0) { accountRepository.existsActiveByNameAndIdNot(any(), any()) }
        }

        @Test
        @DisplayName("T19 SYSTEM_ADMIN + name blank - blank 검증은 적용 → ACCOUNT_NAME_BLANK")
        fun t19_systemAdminStillBlankCheck() {
            assertThatThrownBy {
                service.update(
                    id = accountId,
                    principal = systemAdminPrincipal,
                    request = AdminAccountUpdateRequest(name = "  ")
                )
            }.isInstanceOf(AccountNameBlankException::class.java)
        }
    }
}
