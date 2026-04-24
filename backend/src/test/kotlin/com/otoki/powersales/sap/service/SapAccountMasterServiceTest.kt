package com.otoki.powersales.sap.service

import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.repository.AccountRepository
import com.otoki.powersales.sap.dto.SapAccountMasterRequest.ReqItem
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
@DisplayName("SapAccountMasterService 테스트")
class SapAccountMasterServiceTest {

    @Mock
    private lateinit var accountRepository: AccountRepository

    @InjectMocks
    private lateinit var sapAccountMasterService: SapAccountMasterService

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

            val result = sapAccountMasterService.sync(items)

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
        @DisplayName("전체 필드 매핑 - 유지 대상 필드가 Account에 반영됨")
        fun sync_newAccount_fullFieldMapping() {
            val items = listOf(createReqItem(
                sapAccountCode = "0001234567",
                name = "홍길동 슈퍼",
                accountType = "01",
                accountStatusName = "활성",
                accountGroup = "Z001",
                phone = "02-1234-5678",
                mobilePhone = "010-1234-5678",
                employeeCode = "100234",
                representative = "홍길동",
                zipcode = "06234",
                address1 = "서울시 강남구",
                address2 = "1층",
                branchCode = "1111",
                branchName = "강남지점",
                closingTime1 = "18:00",
                abcType = "A",
                abcTypeCode = "01",
                distribution = "Y",
                werk1Tx = "안양물류"
            ))
            whenever(accountRepository.findByExternalKey("0001234567")).thenReturn(null)
            whenever(accountRepository.save(any<Account>())).thenAnswer { it.getArgument<Account>(0) }

            sapAccountMasterService.sync(items)

            val captor = argumentCaptor<Account>()
            verify(accountRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.accountType).isEqualTo("01")
            assertThat(saved.accountStatusName).isEqualTo("활성")
            assertThat(saved.employeeCode).isEqualTo("100234")
            assertThat(saved.distribution).isEqualTo("Y")
            assertThat(saved.phone).isEqualTo("02-1234-5678")
            assertThat(saved.representative).isEqualTo("홍길동")
            assertThat(saved.branchCode).isEqualTo("1111")
            assertThat(saved.werk1Tx).isEqualTo("안양물류")
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

            val result = sapAccountMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(1)
            assertThat(existing.name).isEqualTo("홍길동 슈퍼 수정")
            assertThat(existing.phone).isEqualTo("02-9999-8888")
            assertThat(existing.updatedAt).isNotNull()
        }
    }

    @Nested
    @DisplayName("sync - 에러 처리")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("sap_account_code 누락 - 해당 레코드 실패")
        fun sync_missingAccountCode_fails() {
            val items = listOf(createReqItem(sapAccountCode = null, name = "테스트"))

            val result = sapAccountMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].error).contains("sap_account_code")
        }

        @Test
        @DisplayName("name 누락 - 해당 레코드 실패")
        fun sync_missingName_fails() {
            val items = listOf(createReqItem(sapAccountCode = "0001234567", name = null))

            val result = sapAccountMasterService.sync(items)

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

            val result = sapAccountMasterService.sync(items)

            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failCount).isEqualTo(1)
            assertThat(result.errors[0].index).isEqualTo(1)
        }
    }

    private fun createReqItem(
        sapAccountCode: String? = null,
        name: String? = null,
        accountType: String? = null,
        accountStatusName: String? = null,
        accountGroup: String? = null,
        phone: String? = null,
        mobilePhone: String? = null,
        employeeCode: String? = null,
        representative: String? = null,
        zipcode: String? = null,
        address1: String? = null,
        address2: String? = null,
        branchCode: String? = null,
        branchName: String? = null,
        closingTime1: String? = null,
        abcType: String? = null,
        abcTypeCode: String? = null,
        distribution: String? = null,
        werk1Tx: String? = null
    ) = ReqItem(
        sapAccountCode = sapAccountCode,
        name = name,
        accountType = accountType,
        accountStatusName = accountStatusName,
        accountGroup = accountGroup,
        phone = phone,
        mobilePhone = mobilePhone,
        employeeCode = employeeCode,
        representative = representative,
        zipcode = zipcode,
        address1 = address1,
        address2 = address2,
        branchCode = branchCode,
        branchName = branchName,
        closingTime1 = closingTime1,
        abcType = abcType,
        abcTypeCode = abcTypeCode,
        distribution = distribution,
        werk1Tx = werk1Tx
    )
}
