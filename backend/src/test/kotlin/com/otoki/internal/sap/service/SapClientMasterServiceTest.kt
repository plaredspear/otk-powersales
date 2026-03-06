package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.dto.SapClientMasterRequest.ReqItem
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapClientMasterService 테스트")
class SapClientMasterServiceTest {

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var orgRepository: OrgRepository

    @InjectMocks
    private lateinit var sapClientMasterService: SapClientMasterService

    @Nested
    @DisplayName("sync - 신규 거래처 등록")
    inner class NewAccountTests {

        @Test
        @DisplayName("정상 등록 - DB에 없는 거래처코드 -> Account Insert")
        fun sync_newAccount_creates() {
            val items = listOf(createReqItem(
                sapAccountCode = "0001234567",
                name = "홍길동 슈퍼"
            ))
            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            val result = sapClientMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failCount).isEqualTo(0)
            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.externalKey).isEqualTo("0001234567")
            assertThat(saved.name).isEqualTo("홍길동 슈퍼")
            assertThat(saved.createdAt).isNotNull()
        }

        @Test
        @DisplayName("전체 필드 매핑 - 모든 요청 필드가 Account에 반영됨")
        fun sync_newAccount_fullFieldMapping() {
            val items = listOf(createReqItem(
                sapAccountCode = "0001234567",
                name = "홍길동 슈퍼",
                accountType = "01",
                accountStatusCode = "A",
                accountStatusName = "활성",
                accountGroup = "Z001",
                phone = "02-1234-5678",
                mobilePhone = "010-1234-5678",
                email = "store@example.com",
                businessType = "도소매",
                businessCategory = "식품",
                employeeCode = "100234",
                businessLicenseNumber = "123-45-67890",
                representative = "홍길동",
                zipcode = "06234",
                address1 = "서울시 강남구",
                address2 = "1층",
                divisionCode = "1100",
                divisionName = "영업본부",
                salesDeptCode = "1110",
                salesDeptName = "수도권영업부",
                branchCode = "1111",
                branchName = "강남지점",
                closingTime1 = "18:00",
                abcType = "A",
                abcTypeCode = "01",
                distribution = "Y",
                consignmentAcc = "N",
                werk1 = "1000",
                werk1Tx = "안양물류"
            ))
            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel5("1111")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel4("1111")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel4("1110")).thenReturn(null)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            sapClientMasterService.sync(items)

            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.accountType).isEqualTo("01")
            assertThat(saved.phone).isEqualTo("02-1234-5678")
            assertThat(saved.email).isEqualTo("store@example.com")
            assertThat(saved.representative).isEqualTo("홍길동")
            assertThat(saved.divisionCode).isEqualTo("1100")
            assertThat(saved.branchCode).isEqualTo("1111")
            assertThat(saved.werk1).isEqualTo("1000")
            assertThat(saved.werk1Tx).isEqualTo("안양물류")
            assertThat(saved.distribution).isEqualTo("Y")
        }
    }

    @Nested
    @DisplayName("sync - 기존 거래처 업데이트")
    inner class ExistingAccountTests {

        @Test
        @DisplayName("기존 거래처 업데이트 - 필드 변경")
        fun sync_existingAccount_updates() {
            val existing = Account(id = 1, externalKey = "0001234567", name = "기존슈퍼")
            val items = listOf(createReqItem(
                sapAccountCode = "0001234567",
                name = "홍길동 슈퍼 수정",
                phone = "02-9999-8888"
            ))
            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(existing)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            val result = sapClientMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existing.name).isEqualTo("홍길동 슈퍼 수정")
            assertThat(existing.phone).isEqualTo("02-9999-8888")
            assertThat(existing.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("sync - 조직코드 매핑")
    inner class OrgMappingTests {

        @Test
        @DisplayName("cc_cd5 매칭 - branch_code로 org 조회 성공")
        fun sync_orgMapping_ccCd5Match() {
            val org = createOrg(orgCd3 = "ORG3", orgCd4 = "ORG4", orgCd5 = "ORG5")
            val items = listOf(createReqItem(sapAccountCode = "0001234567", name = "테스트", branchCode = "1111"))

            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel5("1111")).thenReturn(org)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            sapClientMasterService.sync(items)

            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            assertThat(captor.firstValue.orgCd3).isEqualTo("ORG3")
            assertThat(captor.firstValue.orgCd4).isEqualTo("ORG4")
            assertThat(captor.firstValue.orgCd5).isEqualTo("ORG5")
        }

        @Test
        @DisplayName("cc_cd4 Fallback - cc_cd5 미매칭, cc_cd4 매칭")
        fun sync_orgMapping_ccCd4Fallback() {
            val org = createOrg(orgCd3 = "ORG3", orgCd4 = "ORG4", orgCd5 = null)
            val items = listOf(createReqItem(sapAccountCode = "0001234567", name = "테스트", branchCode = "1111"))

            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel5("1111")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel4("1111")).thenReturn(org)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            sapClientMasterService.sync(items)

            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            assertThat(captor.firstValue.orgCd3).isEqualTo("ORG3")
            assertThat(captor.firstValue.orgCd4).isEqualTo("ORG4")
        }

        @Test
        @DisplayName("salesDeptCode Fallback - branchCode로 모두 미매칭 후 salesDeptCode로 재조회")
        fun sync_orgMapping_salesDeptCodeFallback() {
            val org = createOrg(orgCd3 = "ORG3", orgCd4 = "ORG4", orgCd5 = null)
            val items = listOf(createReqItem(
                sapAccountCode = "0001234567", name = "테스트",
                branchCode = "1111", salesDeptCode = "1110"
            ))

            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel5("1111")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel4("1111")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel4("1110")).thenReturn(org)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            sapClientMasterService.sync(items)

            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            assertThat(captor.firstValue.orgCd3).isEqualTo("ORG3")
        }

        @Test
        @DisplayName("조직 미매칭 - 모든 조회 실패 시 org_cd null")
        fun sync_orgMapping_noMatch() {
            val items = listOf(createReqItem(
                sapAccountCode = "0001234567", name = "테스트",
                branchCode = "9999", salesDeptCode = "9998"
            ))

            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel5("9999")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel4("9999")).thenReturn(null)
            whenever(orgRepository.findFirstByCostCenterLevel4("9998")).thenReturn(null)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            sapClientMasterService.sync(items)

            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            assertThat(captor.firstValue.orgCd3).isNull()
            assertThat(captor.firstValue.orgCd4).isNull()
            assertThat(captor.firstValue.orgCd5).isNull()
        }

        @Test
        @DisplayName("branchCode 없음 - org 조회 스킵, org_cd null")
        fun sync_orgMapping_noBranchCode() {
            val items = listOf(createReqItem(sapAccountCode = "0001234567", name = "테스트"))

            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            sapClientMasterService.sync(items)

            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            assertThat(captor.firstValue.orgCd3).isNull()
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("sap_account_code 누락 - 해당 레코드 실패")
        fun sync_missingAccountCode_fails() {
            val items = listOf(createReqItem(sapAccountCode = null, name = "테스트"))

            val result = sapClientMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("sap_account_code")
        }

        @Test
        @DisplayName("name 누락 - 해당 레코드 실패")
        fun sync_missingName_fails() {
            val items = listOf(createReqItem(sapAccountCode = "0001234567", name = null))

            val result = sapClientMasterService.sync(items)

            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("name")
        }

        @Test
        @DisplayName("부분 실패 - 3건 중 1건 에러")
        fun sync_partialFailure() {
            val items = listOf(
                createReqItem(sapAccountCode = "0001", name = "성공1"),
                createReqItem(sapAccountCode = null, name = "실패"),
                createReqItem(sapAccountCode = "0003", name = "성공2")
            )
            whenever(accountRepository.findByExternalKey("0001")).thenReturn(null)
            whenever(accountRepository.findByExternalKey("0003")).thenReturn(null)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            val result = sapClientMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    private fun createReqItem(
        sapAccountCode: String? = null,
        name: String? = null,
        accountType: String? = null,
        accountStatusCode: String? = null,
        accountStatusName: String? = null,
        accountGroup: String? = null,
        phone: String? = null,
        mobilePhone: String? = null,
        email: String? = null,
        businessType: String? = null,
        businessCategory: String? = null,
        employeeCode: String? = null,
        businessLicenseNumber: String? = null,
        representative: String? = null,
        zipcode: String? = null,
        address1: String? = null,
        address2: String? = null,
        divisionCode: String? = null,
        divisionName: String? = null,
        salesDeptCode: String? = null,
        salesDeptName: String? = null,
        branchCode: String? = null,
        branchName: String? = null,
        closingTime1: String? = null,
        abcType: String? = null,
        abcTypeCode: String? = null,
        distribution: String? = null,
        consignmentAcc: String? = null,
        werk1: String? = null,
        werk1Tx: String? = null
    ) = ReqItem(
        sapAccountCode = sapAccountCode,
        name = name,
        accountType = accountType,
        accountStatusCode = accountStatusCode,
        accountStatusName = accountStatusName,
        accountGroup = accountGroup,
        phone = phone,
        mobilePhone = mobilePhone,
        email = email,
        businessType = businessType,
        businessCategory = businessCategory,
        employeeCode = employeeCode,
        businessLicenseNumber = businessLicenseNumber,
        representative = representative,
        zipcode = zipcode,
        address1 = address1,
        address2 = address2,
        divisionCode = divisionCode,
        divisionName = divisionName,
        salesDeptCode = salesDeptCode,
        salesDeptName = salesDeptName,
        branchCode = branchCode,
        branchName = branchName,
        closingTime1 = closingTime1,
        abcType = abcType,
        abcTypeCode = abcTypeCode,
        distribution = distribution,
        consignmentAcc = consignmentAcc,
        werk1 = werk1,
        werk1Tx = werk1Tx
    )

    private fun createOrg(
        orgCd3: String? = null,
        orgCd4: String? = null,
        orgCd5: String? = null
    ) = Org(
        orgCodeLevel3 = orgCd3,
        orgCodeLevel4 = orgCd4,
        orgCodeLevel5 = orgCd5
    )
}
