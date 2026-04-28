package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.entity.Organization
import com.otoki.powersales.sap.inbound.dto.account.ClientMasterRequestItem
import com.otoki.powersales.sap.repository.AccountRepository
import com.otoki.powersales.sap.repository.EmployeeRepository
import com.otoki.powersales.sap.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapClientMasterService 테스트")
class SapClientMasterServiceTest {

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapClientMasterService

    private fun item(
        sapAccountCode: String? = "1032619",
        name: String? = "(주)홍길동상회",
        employeeCode: String? = null,
        branchCode: String? = null,
        salesDeptCode: String? = null,
        divisionCode: String? = null,
        branchName: String? = null,
        phone: String? = null
    ): ClientMasterRequestItem = ClientMasterRequestItem(
        sapAccountCode = sapAccountCode,
        name = name,
        employeeCode = employeeCode,
        branchCode = branchCode,
        salesDeptCode = salesDeptCode,
        divisionCode = divisionCode,
        branchName = branchName,
        phone = phone
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
            whenever(accountRepository.findByExternalKeyIn(listOf("1032619"))).thenReturn(emptyList())
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val detail = service.upsert(listOf(item()))

            val captor = argumentCaptor<List<Account>>()
            verify(accountRepository).saveAll(captor.capture())
            assertThat(captor.firstValue).hasSize(1)
            assertThat(captor.firstValue[0].externalKey).isEqualTo("1032619")
            assertThat(captor.firstValue[0].name).isEqualTo("(주)홍길동상회")
            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
            verify(auditService).record(any<SapInboundAudit>())
        }

        @Test
        @DisplayName("기존 거래처 갱신 - 동일 externalKey, name/phone 업데이트")
        fun upsert_updateExisting() {
            val existing = Account(externalKey = "1032619", name = "기존이름", phone = "old-phone")
            whenever(accountRepository.findByExternalKeyIn(listOf("1032619"))).thenReturn(listOf(existing))
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            service.upsert(listOf(item(name = "새이름", phone = "new-phone")))

            val captor = argumentCaptor<List<Account>>()
            verify(accountRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
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
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>())).thenReturn(emptyList())
            whenever(organizationRepository.findAll()).thenReturn(listOf(org))

            service.upsert(listOf(item(branchCode = "1111", branchName = "raw-name")))

            val captor = argumentCaptor<List<Account>>()
            verify(accountRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().branchCode).isEqualTo("11110")
            assertThat(captor.firstValue.single().branchName).isEqualTo("서울지점")
        }

        @Test
        @DisplayName("Organization 폴백 (BranchCode 없음 + SalesDeptCode 매칭)")
        fun upsert_orgFallbackToSalesDept() {
            val org = organization(
                cc4 = "1110",
                oc4 = "11100", oc5 = null,
                on4 = "영업1팀", on5 = null
            )
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>())).thenReturn(emptyList())
            whenever(organizationRepository.findAll()).thenReturn(listOf(org))

            service.upsert(listOf(item(salesDeptCode = "1110")))

            val captor = argumentCaptor<List<Account>>()
            verify(accountRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().branchCode).isEqualTo("11100")
            assertThat(captor.firstValue.single().branchName).isEqualTo("영업1팀")
        }

        @Test
        @DisplayName("Organization 매칭 실패 - 페이로드 raw 값 그대로 저장")
        fun upsert_orgMissingFallbackRaw() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>())).thenReturn(emptyList())
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            service.upsert(listOf(item(branchCode = "9999", branchName = "raw-branch")))

            val captor = argumentCaptor<List<Account>>()
            verify(accountRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().branchCode).isEqualTo("9999")
            assertThat(captor.firstValue.single().branchName).isEqualTo("raw-branch")
        }

        @Test
        @DisplayName("EmployeeCode 매칭 성공 - 거래처 적재")
        fun upsert_employeeMatched() {
            val employee = Employee(employeeCode = "100123", name = "홍길동")
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>())).thenReturn(emptyList())
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(listOf(employee))
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(employeeCode = "100123")))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
            val captor = argumentCaptor<List<Account>>()
            verify(accountRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().employeeCode).isEqualTo("100123")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("EmployeeCode 매칭 실패 - failures 에 기록, 적재 스킵")
        fun upsert_employeeNotFound() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>())).thenReturn(emptyList())
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("999999"))).thenReturn(emptyList())
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(employeeCode = "999999")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("1032619")
            assertThat(detail.failures.single().reason).contains("employee_code not found")
            verify(accountRepository, never()).saveAll(any<List<Account>>())
        }

        @Test
        @DisplayName("SAPAccountCode 누락 - failures 에 기록, identifier null")
        fun upsert_missingSapAccountCode() {
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(sapAccountCode = null)))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isNull()
            assertThat(detail.failures.single().reason).contains("SAPAccountCode 필수")
        }

        @Test
        @DisplayName("Name 누락 - failures 에 기록")
        fun upsert_missingName() {
            whenever(accountRepository.findByExternalKeyIn(any<List<String>>())).thenReturn(emptyList())
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(name = null)))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("1032619")
            assertThat(detail.failures.single().reason).contains("Name 필수")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            whenever(accountRepository.findByExternalKeyIn(listOf("A", "B"))).thenReturn(emptyList())
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val detail = service.upsert(
                listOf(
                    item(sapAccountCode = "A", name = "정상"),
                    item(sapAccountCode = "B", name = null)
                )
            )

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().identifier).isEqualTo("B")
        }
    }
}
