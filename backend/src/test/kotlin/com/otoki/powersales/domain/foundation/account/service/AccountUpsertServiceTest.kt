package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AccountUpsertService 테스트")
class AccountUpsertServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val mapper: AccountUpsertMapper = AccountUpsertMapper()

    private val service = AccountUpsertService(
        accountRepository,
        employeeRepository,
        organizationRepository,
        userRepository,
        mapper,
    )

    @BeforeEach
    fun setUpDefaults() {
        every { employeeRepository.findByEmployeeCodeIn(any()) } returns emptyList()
        every { userRepository.findByEmployeeCodeIn(any()) } returns emptyList()
    }

    private fun stubSaveAllCapture(): CapturingSlot<List<Account>> {
        val slot = slot<List<Account>>()
        every { accountRepository.saveAll(capture(slot)) } answers { firstArg<List<Account>>() }
        return slot
    }

    private fun command(
        externalKey: String? = "1032619",
        name: String? = "(주)홍길동상회",
        employeeCode: String? = null,
        branchCode: String? = null,
        salesDeptCode: String? = null,
        divisionCode: String? = null,
        branchName: String? = null,
        phone: String? = null,
        accountStatusCode: String? = null,
        businessType: String? = null,
        businessCategory: String? = null,
        businessLicenseNumber: String? = null,
        email: String? = null,
        divisionName: String? = null,
        salesDeptName: String? = null,
        consignmentAcc: String? = null,
        werk1: String? = null,
        werk2: String? = null,
        werk3: String? = null
    ): AccountUpsertCommand = AccountUpsertCommand(
        externalKey = externalKey,
        name = name,
        accountType = null,
        accountStatusCode = accountStatusCode,
        accountStatusName = null,
        accountGroup = null,
        phone = phone,
        mobilePhone = null,
        email = email,
        businessType = businessType,
        businessCategory = businessCategory,
        employeeCode = employeeCode,
        businessLicenseNumber = businessLicenseNumber,
        representative = null,
        zipcode = null,
        address1 = null,
        address2 = null,
        divisionCode = divisionCode,
        divisionName = divisionName,
        salesDeptCode = salesDeptCode,
        salesDeptName = salesDeptName,
        branchCode = branchCode,
        branchName = branchName,
        closingTime1 = null,
        closingTime2 = null,
        closingTime3 = null,
        abcType = null,
        abcTypeCode = null,
        distribution = null,
        consignmentAcc = consignmentAcc,
        werk1 = werk1,
        werk2 = werk2,
        werk3 = werk3,
        werk1Tx = null,
        werk2Tx = null,
        werk3Tx = null
    )

    private fun organization(
        cc3: String? = null,
        cc4: String? = null,
        cc5: String? = null,
        oc3: String? = null,
        oc4: String? = null,
        oc5: String? = null,
        on3: String? = null,
        on4: String? = null,
        on5: String? = null
    ): Organization = Organization(
        costCenterLevel3 = cc3,
        costCenterLevel4 = cc4,
        costCenterLevel5 = cc5,
        orgCodeLevel3 = oc3,
        orgCodeLevel4 = oc4,
        orgCodeLevel5 = oc5,
        orgNameLevel3 = on3,
        orgNameLevel4 = on4,
        orgNameLevel5 = on5
    )

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 거래처 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            every { accountRepository.findByExternalKeyIn(listOf("1032619")) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command()))

            assertThat(savedSlot.captured).hasSize(1)
            assertThat(savedSlot.captured[0].externalKey).isEqualTo("1032619")
            assertThat(savedSlot.captured[0].name).isEqualTo("(주)홍길동상회")
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 거래처 갱신 - 동일 externalKey, name/phone 업데이트")
        fun upsert_updateExisting() {
            val existing = Account(externalKey = "1032619", name = "기존이름", phone = "old-phone")
            every { accountRepository.findByExternalKeyIn(listOf("1032619")) } returns listOf(existing)
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(name = "새이름", phone = "new-phone")))

            val saved = savedSlot.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("새이름")
            assertThat(saved.phone).isEqualTo("new-phone")
        }

        @Test
        @DisplayName("Organization 매칭 (BranchCode = level5) - org 의 OrgCode/OrgName 으로 채워짐")
        fun upsert_orgMatchByBranchCode() {
            val org = organization(
                cc3 = "1100", cc4 = "1110", cc5 = "1111",
                oc3 = "11000", oc4 = "11100", oc5 = "11110",
                on3 = "Retail사업부", on4 = "영업1팀", on5 = "서울지점"
            )
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns listOf(org)
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(branchCode = "1111", branchName = "raw-name")))

            assertThat(savedSlot.captured.single().branchCode).isEqualTo("11110")
            assertThat(savedSlot.captured.single().branchName).isEqualTo("서울지점")
        }

        @Test
        @DisplayName("Organization 폴백 (BranchCode 없음 + SalesDeptCode 매칭)")
        fun upsert_orgFallbackToSalesDept() {
            val org = organization(
                cc4 = "1110",
                oc4 = "11100", oc5 = null,
                on4 = "영업1팀", on5 = null
            )
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns listOf(org)
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(salesDeptCode = "1110")))

            assertThat(savedSlot.captured.single().branchCode).isEqualTo("11100")
            assertThat(savedSlot.captured.single().branchName).isEqualTo("영업1팀")
        }

        @Test
        @DisplayName("Organization 매칭 실패 - 페이로드 raw 값 그대로 저장")
        fun upsert_orgMissingFallbackRaw() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(branchCode = "9999", branchName = "raw-branch")))

            assertThat(savedSlot.captured.single().branchCode).isEqualTo("9999")
            assertThat(savedSlot.captured.single().branchName).isEqualTo("raw-branch")
        }

        @Test
        @DisplayName("EmployeeCode 매칭 성공 - 거래처 적재")
        fun upsert_employeeMatched() {
            val employee = Employee(employeeCode = "100123", name = "홍길동")
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { employeeRepository.findByEmployeeCodeIn(listOf("100123")) } returns listOf(employee)
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(employeeCode = "100123")))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.single().employeeCode).isEqualTo("100123")
        }

        @Test
        @DisplayName("Spec #575 - 13개 레거시 필드 모두 entity 에 저장됨")
        fun upsert_legacyFields_allMapped() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    command(
                        accountStatusCode = "01",
                        businessType = "유통업",
                        businessCategory = "도매",
                        businessLicenseNumber = "123-45-67890",
                        email = "shop@test.com",
                        divisionName = "제1사업부",
                        salesDeptName = "수도권영업팀",
                        consignmentAcc = "Y",
                        werk1 = "1100",
                        werk2 = "1200",
                        werk3 = "1300",
                        salesDeptCode = "1110",
                        divisionCode = "1100"
                    )
                )
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.accountStatusCode).isEqualTo("01")
            assertThat(saved.businessType).isEqualTo("유통업")
            assertThat(saved.businessCategory).isEqualTo("도매")
            assertThat(saved.businessLicenseNumber).isEqualTo("123-45-67890")
            assertThat(saved.email).isEqualTo("shop@test.com")
            assertThat(saved.divisionName).isEqualTo("제1사업부")
            assertThat(saved.salesDeptName).isEqualTo("수도권영업팀")
            assertThat(saved.consignmentAcc).isEqualTo("Y")
            assertThat(saved.werk1).isEqualTo("1100")
            assertThat(saved.werk2).isEqualTo("1200")
            assertThat(saved.werk3).isEqualTo("1300")
            assertThat(saved.salesDeptCostCenter).isEqualTo("1110")
            assertThat(saved.divisionCostCenter).isEqualTo("1100")
        }

        @Test
        @DisplayName("Spec #575 - 13개 레거시 필드 빈 문자열 → 빈 문자열 그대로 저장")
        fun upsert_legacyFields_blankPreserved() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    command(
                        accountStatusCode = "",
                        businessType = "",
                        businessCategory = "",
                        businessLicenseNumber = "",
                        email = "",
                        divisionName = "",
                        salesDeptName = "",
                        consignmentAcc = "",
                        werk1 = "",
                        werk2 = "",
                        werk3 = ""
                    )
                )
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.accountStatusCode).isEqualTo("")
            assertThat(saved.consignmentAcc).isEqualTo("")
            assertThat(saved.werk1).isEqualTo("")
        }

        @Test
        @DisplayName("Spec #575 - ConsignmentAcc=N 정상 저장")
        fun upsert_consignmentAccN_pass() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            stubSaveAllCapture()

            val result = service.upsert(listOf(command(consignmentAcc = "N")))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("EmployeeCode 매칭 실패 - failures 에 기록, 적재 스킵")
        fun upsert_employeeNotFound() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { employeeRepository.findByEmployeeCodeIn(listOf("999999")) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()

            val result = service.upsert(listOf(command(employeeCode = "999999")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("1032619")
            assertThat(result.failures.single().reason).contains("employee_code not found")
            verify(exactly = 0) { accountRepository.saveAll(any<List<Account>>()) }
        }

        @Test
        @DisplayName("SAPAccountCode 누락 - failures 에 기록, identifier null")
        fun upsert_missingExternalKey() {
            every { organizationRepository.findAll() } returns emptyList()
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { accountRepository.saveAll(any<List<Account>>()) } answers { firstArg<List<Account>>() }

            val result = service.upsert(listOf(command(externalKey = null)))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("SAPAccountCode 필수")
        }

        @Test
        @DisplayName("Name 누락 - failures 에 기록")
        fun upsert_missingName() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            every { accountRepository.saveAll(any<List<Account>>()) } answers { firstArg<List<Account>>() }

            val result = service.upsert(listOf(command(name = null)))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("1032619")
            assertThat(result.failures.single().reason).contains("Name 필수")
        }

        @Test
        @DisplayName("Spec #575 - ConsignmentAcc 형식 위반 (소문자 'y') → 행 단위 부분 실패")
        fun upsert_consignmentAccInvalidLowercase() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            every { accountRepository.saveAll(any<List<Account>>()) } answers { firstArg<List<Account>>() }

            val result = service.upsert(listOf(command(consignmentAcc = "y")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("1032619")
            assertThat(result.failures.single().reason).contains("ConsignmentAcc 형식 오류: y")
            verify(exactly = 0) { accountRepository.saveAll(any<List<Account>>()) }
        }

        @Test
        @DisplayName("Spec #575 - ConsignmentAcc 형식 위반 (2자) → 행 단위 부분 실패")
        fun upsert_consignmentAccInvalidLength() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            every { accountRepository.saveAll(any<List<Account>>()) } answers { firstArg<List<Account>>() }

            val result = service.upsert(listOf(command(consignmentAcc = "YN")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("ConsignmentAcc 형식 오류: YN")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            every { accountRepository.findByExternalKeyIn(listOf("A", "B")) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            stubSaveAllCapture()

            val result = service.upsert(
                listOf(
                    command(externalKey = "A", name = "정상"),
                    command(externalKey = "B", name = null)
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("B")
        }
    }

    @Nested
    @DisplayName("Spec #646 - Org Code 트리오 매핑 (divisionCode/salesDeptCode/branchCostCenter)")
    inner class UpsertOrgCodeTrio {

        @Test
        @DisplayName("O1 Org Level5 매칭 - divisionCode/salesDeptCode/branchCode 모두 ORG5, branchCostCenter=raw")
        fun upsert_orgCodeTrio_level5Match() {
            val org = organization(
                cc5 = "BR001",
                oc3 = "ORG3", oc4 = "ORG4", oc5 = "ORG5",
                on3 = "사업부", on4 = "영업팀", on5 = "지점"
            )
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns listOf(org)
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    command(
                        branchCode = "BR001",
                        divisionCode = "DIV1",
                        salesDeptCode = "SD1"
                    )
                )
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.divisionCode).isEqualTo("ORG5")
            assertThat(saved.salesDeptCode).isEqualTo("ORG5")
            assertThat(saved.branchCode).isEqualTo("ORG5")
            assertThat(saved.branchCostCenter).isEqualTo("BR001")
            assertThat(saved.divisionCostCenter).isEqualTo("DIV1")
            assertThat(saved.salesDeptCostCenter).isEqualTo("SD1")
        }

        @Test
        @DisplayName("O2 Org Level4 매칭 (Level5 blank) - 트리오 모두 ORG4")
        fun upsert_orgCodeTrio_level4Match() {
            val org = organization(
                cc5 = "BR001",
                oc3 = "ORG3", oc4 = "ORG4", oc5 = "",
                on3 = "사업부", on4 = "영업팀", on5 = ""
            )
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns listOf(org)
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    command(
                        branchCode = "BR001",
                        divisionCode = "DIV1",
                        salesDeptCode = "SD1"
                    )
                )
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.divisionCode).isEqualTo("ORG4")
            assertThat(saved.salesDeptCode).isEqualTo("ORG4")
            assertThat(saved.branchCode).isEqualTo("ORG4")
            assertThat(saved.branchCostCenter).isEqualTo("BR001")
        }

        @Test
        @DisplayName("O3 Org Level3 매칭 (Level5/4 blank) - 트리오 모두 ORG3")
        fun upsert_orgCodeTrio_level3Match() {
            val org = organization(
                cc5 = "BR001",
                oc3 = "ORG3", oc4 = "", oc5 = "",
                on3 = "사업부", on4 = "", on5 = ""
            )
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns listOf(org)
            val savedSlot = stubSaveAllCapture()

            service.upsert(
                listOf(
                    command(
                        branchCode = "BR001",
                        divisionCode = "DIV1",
                        salesDeptCode = "SD1"
                    )
                )
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.divisionCode).isEqualTo("ORG3")
            assertThat(saved.salesDeptCode).isEqualTo("ORG3")
            assertThat(saved.branchCode).isEqualTo("ORG3")
            assertThat(saved.branchCostCenter).isEqualTo("BR001")
        }

        @Test
        @DisplayName("O4 Org 미매칭 - divisionCode/salesDeptCode=null, branchCode=raw, branchCostCenter=raw")
        fun upsert_orgCodeTrio_noMatch() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(branchCode = "BR999")))

            val saved = savedSlot.captured.single()
            assertThat(saved.divisionCode).isNull()
            assertThat(saved.salesDeptCode).isNull()
            assertThat(saved.branchCode).isEqualTo("BR999")
            assertThat(saved.branchCostCenter).isEqualTo("BR999")
        }

        @Test
        @DisplayName("O5 branchCode 빈값 - 트리오 + branchCode + branchCostCenter 모두 null")
        fun upsert_orgCodeTrio_blankBranchCode() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(branchCode = "")))

            val saved = savedSlot.captured.single()
            assertThat(saved.branchCode).isNull()
            assertThat(saved.branchCostCenter).isNull()
            assertThat(saved.divisionCode).isNull()
            assertThat(saved.salesDeptCode).isNull()
        }
    }

    @Nested
    @DisplayName("Spec #758 - Owner FK 매핑 (User)")
    inner class UpsertOwnerFk {

        private fun user(employeeCode: String, username: String = "$employeeCode@otoki.local"): User =
            User(username = username, employeeCode = employeeCode, password = "encoded")

        @Test
        @DisplayName("O1 정상 owner 매핑 -  account.ownerUser = 매칭 User (employee_code 매칭)")
        fun upsert_ownerMapped() {
            val employee = Employee(employeeCode = "12345", name = "홍길동")
            val matchedUser = user("12345")
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { employeeRepository.findByEmployeeCodeIn(listOf("12345")) } returns listOf(employee)
            every { userRepository.findByEmployeeCodeIn(listOf("12345")) } returns listOf(matchedUser)
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(employeeCode = "12345")))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.single().ownerUser).isSameAs(matchedUser)
        }

        @Test
        @DisplayName("O2 Employee 미존재 - 행 차단, owner 매핑 도달 안 함")
        fun upsert_employeeMissing_blocksRow() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { employeeRepository.findByEmployeeCodeIn(listOf("99999")) } returns emptyList()
            every { userRepository.findByEmployeeCodeIn(listOf("99999")) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()

            val result = service.upsert(listOf(command(employeeCode = "99999")))

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("employee_code not found: 99999")
            verify(exactly = 0) { accountRepository.saveAll(any<List<Account>>()) }
        }

        @Test
        @DisplayName("O3 employeeCode blank/null -  account.ownerUser = null 통과")
        fun upsert_employeeCodeBlank_ownerNull() {
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(employeeCode = null)))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(savedSlot.captured.single().ownerUser).isNull()
        }

        @Test
        @DisplayName("O4 Employee 존재 but User 미존재 -  account.ownerUser = null (Employee invariant 미적재 케이스)")
        fun upsert_userMissing_ownerNull() {
            val employee = Employee(employeeCode = "12345", name = "홍길동")
            every { accountRepository.findByExternalKeyIn(any<List<String>>()) } returns emptyList()
            every { employeeRepository.findByEmployeeCodeIn(listOf("12345")) } returns listOf(employee)
            every { userRepository.findByEmployeeCodeIn(listOf("12345")) } returns emptyList()
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(employeeCode = "12345")))

            assertThat(result.successCount).isEqualTo(1)
            assertThat(savedSlot.captured.single().ownerUser).isNull()
        }

        @Test
        @DisplayName("O5 다건 + 일부 owner miss - 통과 행만 owner set, 실패 행 failures 누적")
        fun upsert_partialOwnerMiss() {
            val employee = Employee(employeeCode = "12345", name = "홍길동")
            val matchedUser = user("12345")
            every { accountRepository.findByExternalKeyIn(listOf("A", "B", "C")) } returns emptyList()
            every { employeeRepository.findByEmployeeCodeIn(listOf("12345", "99999")) } returns listOf(employee)
            every { userRepository.findByEmployeeCodeIn(listOf("12345", "99999")) } returns listOf(matchedUser)
            every { organizationRepository.findAll() } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(
                listOf(
                    command(externalKey = "A", employeeCode = "12345"),
                    command(externalKey = "B", employeeCode = "99999"),
                    command(externalKey = "C", employeeCode = null)
                )
            )

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("B")
            val saved = savedSlot.captured
            assertThat(saved).hasSize(2)
            assertThat(saved.first { it.externalKey == "A" }.ownerUser).isSameAs(matchedUser)
            assertThat(saved.first { it.externalKey == "C" }.ownerUser).isNull()
        }
    }
}
