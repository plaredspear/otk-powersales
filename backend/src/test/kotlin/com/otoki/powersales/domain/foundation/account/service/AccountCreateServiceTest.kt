package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.exception.AccountNameBlankException
import com.otoki.powersales.domain.foundation.account.exception.AccountNameDuplicateException
import com.otoki.powersales.domain.foundation.account.exception.AccountNamePrefixRequiredException
import com.otoki.powersales.domain.foundation.account.exception.EmployeeCodeBlankException
import com.otoki.powersales.domain.foundation.account.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Optional

@DisplayName("AccountCreateService 테스트 (Spec #640 P1-B)")
class AccountCreateServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()

    private val service = AccountCreateService(
        accountRepository,
        employeeRepository,
        organizationRepository,
    )

    private val employeeCode = "100123"
    private val costCenterCode = "CC100"

    private lateinit var savedSlot: CapturingSlot<Account>

    @BeforeEach
    fun setUp() {
        savedSlot = slot()
        every { accountRepository.save(capture(savedSlot)) } answers {
            val account = firstArg<Account>()
            Account(
                id = 1234,
                name = account.name,
                accountGroup = account.accountGroup,
                externalKey = account.externalKey,
                employeeCode = account.employeeCode,
                branchCode = account.branchCode,
                branchName = account.branchName
            )
        }
        every { accountRepository.existsActiveByName(any()) } returns false
        every { organizationRepository.findAll() } returns emptyList()
    }

    private fun employeeWith(
        cost: String? = costCenterCode,
        deleted: Boolean? = null
    ): Employee = Employee(
        id = 10,
        employeeCode = employeeCode,
        name = "홍길동",
        costCenterCode = cost,
        isDeleted = deleted
    )

    private fun org(
        cc5: String? = null,
        cc4: String? = null,
        cc3: String? = null,
        nm5: String? = null,
        nm4: String? = null,
        nm3: String? = null
    ): Organization = Organization(
        costCenterLevel5 = cc5,
        costCenterLevel4 = cc4,
        costCenterLevel3 = cc3,
        orgNameLevel5 = nm5,
        orgNameLevel4 = nm4,
        orgNameLevel3 = nm3
    )

    private fun mockEmployee(employee: Employee = employeeWith()) {
        every { employeeRepository.findByEmployeeCode(employeeCode) } returns Optional.of(employee)
    }

    private fun mockOrgs(vararg orgs: Organization) {
        every { organizationRepository.findAll() } returns orgs.toList()
    }

    @Nested
    @DisplayName("정상 등록 / 자동 set")
    inner class HappyPath {

        @Test
        @DisplayName("T1 정상 - name '(신규) 강남점' + employeeCode '100123' + Organization 매칭 → account_group=9999, branch_code=cost_center, branch_name=org_name")
        fun t1_success() {
            mockEmployee()
            mockOrgs(org(cc5 = costCenterCode, nm5 = "강남지점"))

            val response = service.create(
                AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode)
            )

            assertThat(response.id).isEqualTo(1234)
            assertThat(response.name).isEqualTo("(신규) 강남점")
            assertThat(response.accountGroup).isEqualTo("9999")
            assertThat(response.employeeCode).isEqualTo(employeeCode)
            assertThat(response.branchCode).isEqualTo(costCenterCode)
            assertThat(response.branchName).isEqualTo("강남지점")

            val saved = savedSlot.captured
            assertThat(saved.externalKey).isNull()
            assertThat(saved.accountGroup).isEqualTo("9999")
        }
    }

    @Nested
    @DisplayName("입력 검증 — name")
    inner class NameValidation {

        @Test
        @DisplayName("T2 name blank - ACCOUNT_NAME_BLANK (양끝 공백 trim 후)")
        fun t2_nameBlank() {
            assertThatThrownBy {
                service.create(AdminAccountCreateRequest(name = "   ", employeeCode = employeeCode))
            }.isInstanceOf(AccountNameBlankException::class.java)

            verify(exactly = 0) { accountRepository.save(any<Account>()) }
        }

        @Test
        @DisplayName("T4 name prefix 미포함 ('강남점' 단독) - ACCOUNT_NAME_PREFIX_REQUIRED + 메시지 '(신규)/(기타)'")
        fun t4_prefixMissing() {
            assertThatThrownBy {
                service.create(AdminAccountCreateRequest(name = "강남점", employeeCode = employeeCode))
            }
                .isInstanceOf(AccountNamePrefixRequiredException::class.java)
                .hasMessage("신규 거래처 등록은 ((신규)/(기타)) 중 1개를 필수로 입력하셔야 합니다.")
        }

        @Test
        @DisplayName("T5 name prefix 포함 + 동일명 활성 row 존재 - ACCOUNT_NAME_DUPLICATE")
        fun t5_duplicate() {
            every { accountRepository.existsActiveByName("(신규) 강남점") } returns true

            assertThatThrownBy {
                service.create(AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode))
            }.isInstanceOf(AccountNameDuplicateException::class.java)

            verify(exactly = 0) { accountRepository.save(any<Account>()) }
        }

        @Test
        @DisplayName("T6 name 동일하지만 기존 row 가 is_deleted=true - existsActiveByName=false → 성공")
        fun t6_softDeletedOk() {
            every { accountRepository.existsActiveByName("(신규) 강남점") } returns false
            mockEmployee()
            mockOrgs()

            val response = service.create(
                AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode)
            )

            assertThat(response.name).isEqualTo("(신규) 강남점")
            verify { accountRepository.save(any<Account>()) }
        }

        @Test
        @DisplayName("T6-N name 동일 + 기존 row 가 is_deleted IS NULL - existsActiveByName=true (NULL = 활성) → ACCOUNT_NAME_DUPLICATE")
        fun t6n_nullDeletedAsActive() {
            every { accountRepository.existsActiveByName("(신규) 강남점") } returns true

            assertThatThrownBy {
                service.create(AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode))
            }.isInstanceOf(AccountNameDuplicateException::class.java)
        }
    }

    @Nested
    @DisplayName("입력 검증 — employee")
    inner class EmployeeValidation {

        @Test
        @DisplayName("T7 employeeCode blank - EMPLOYEE_CODE_BLANK")
        fun t7_employeeBlank() {
            assertThatThrownBy {
                service.create(AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = "  "))
            }.isInstanceOf(EmployeeCodeBlankException::class.java)
        }

        @Test
        @DisplayName("T8 Employee 미등록 - EMPLOYEE_NOT_FOUND + 메시지에 employeeCode 포함")
        fun t8_employeeNotFound() {
            every { employeeRepository.findByEmployeeCode("99999") } returns Optional.empty()

            assertThatThrownBy {
                service.create(AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = "99999"))
            }
                .isInstanceOf(EmployeeNotFoundException::class.java)
                .hasMessage("담당 영업사원을 찾을 수 없습니다: 99999")
        }

        @Test
        @DisplayName("T8-soft Employee 가 is_deleted=true 인 경우도 EMPLOYEE_NOT_FOUND")
        fun t8_softDeletedTreatedAsNotFound() {
            every { employeeRepository.findByEmployeeCode(employeeCode) } returns Optional.of(employeeWith(deleted = true))

            assertThatThrownBy {
                service.create(AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode))
            }.isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("Organization 매칭")
    inner class OrganizationMatching {

        @Test
        @DisplayName("T9 Employee.cost_center_code 가 Organization 미매칭 - branch_code 폴백 + branch_name=NULL")
        fun t9_orgMissFallback() {
            mockEmployee()
            mockOrgs(org(cc5 = "OTHER", nm5 = "다른지점"))

            val response = service.create(
                AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode)
            )

            assertThat(response.branchCode).isEqualTo(costCenterCode)
            assertThat(response.branchName).isNull()
        }

        @Test
        @DisplayName("T10 level5 hit - branch_name=org_name_level5")
        fun t10_level5Hit() {
            mockEmployee()
            mockOrgs(org(cc5 = costCenterCode, nm5 = "L5", nm4 = "L4", nm3 = "L3"))

            val response = service.create(
                AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode)
            )

            assertThat(response.branchName).isEqualTo("L5")
        }

        @Test
        @DisplayName("T11 level5 blank, level4 hit - branch_name=org_name_level4")
        fun t11_level4Hit() {
            mockEmployee()
            mockOrgs(org(cc5 = null, cc4 = costCenterCode, nm5 = null, nm4 = "L4", nm3 = "L3"))

            val response = service.create(
                AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode)
            )

            assertThat(response.branchName).isEqualTo("L4")
        }

        @Test
        @DisplayName("T12 level5/4 blank, level3 hit - branch_name=org_name_level3")
        fun t12_level3Hit() {
            mockEmployee()
            mockOrgs(org(cc5 = null, cc4 = null, cc3 = costCenterCode, nm5 = null, nm4 = null, nm3 = "L3"))

            val response = service.create(
                AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = employeeCode)
            )

            assertThat(response.branchName).isEqualTo("L3")
        }
    }
}
