package com.otoki.powersales.account.service

import com.otoki.powersales.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.exception.AccountNameBlankException
import com.otoki.powersales.account.exception.AccountNameDuplicateException
import com.otoki.powersales.account.exception.AccountNamePrefixRequiredForUpdateException
import com.otoki.powersales.account.exception.AccountNotFoundException
import com.otoki.powersales.account.exception.EmployeeNotFoundException
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AccountUpdateService 테스트 (Spec #643 P1-B)")
class AccountUpdateServiceTest {

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @InjectMocks
    private lateinit var service: AccountUpdateService

    private val accountId = 1234
    private fun webPrincipal(userId: Long, role: UserRole) = WebUserPrincipal(
        userId = userId,
        usernameValue = "u$userId@otokims.co.kr",
        employeeNumber = "S$userId",
        employeeId = userId,
        role = role,
        profileType = ProfileType.STAFF,
        isSalesSupport = false,
        passwordChangeRequired = false,
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true
    )

    private val branchManagerPrincipal = webPrincipal(1L, UserRole.BRANCH_MANAGER)
    private val systemAdminPrincipal = webPrincipal(9L, UserRole.SYSTEM_ADMIN)

    private fun nativeAccount(
        id: Int = accountId,
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
        whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(nativeAccount())
    }

    @Nested
    @DisplayName("정상 수정 / PUT 부분 갱신 시맨틱")
    inner class HappyPathAndPartialUpdate {

        @Test
        @DisplayName("T1 정상 수정 - name + address1 + phone 동시 갱신")
        fun t1_success() {
            val account = nativeAccount(address1 = "기존 주소", phone = "02-0000-0000")
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

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
            // entity 도 갱신되었는지
            assertThat(account.name).isEqualTo("(신규) 강남점 신호 수정")
            assertThat(account.address1).isEqualTo("서울특별시 강남구 테헤란로 100")
        }

        @Test
        @DisplayName("T2 빈 페이로드 PUT (모든 필드 null) - 변경 0건 + 200 OK")
        fun t2_emptyPayload() {
            val account = nativeAccount()
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)
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
            // name 미포함이라 prefix/중복 검증 미발동
            verify(accountRepository, never()).existsActiveByNameAndIdNot(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        }

        @Test
        @DisplayName("T3 name 만 포함 - name 만 갱신 + 다른 필드 보존")
        fun t3_nameOnly() {
            val account = nativeAccount(address1 = "기존 주소")
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 강남점 신호 수정")
            )

            assertThat(account.name).isEqualTo("(신규) 강남점 신호 수정")
            assertThat(account.address1).isEqualTo("기존 주소") // 보존
        }

        @Test
        @DisplayName("T4 name 미포함 PUT - prefix 검증 skip 확인 (다른 필드만 변경)")
        fun t4_nameOmitted_prefixSkipped() {
            val account = nativeAccount()
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(phone = "02-9999-9999")
            )

            assertThat(account.phone).isEqualTo("02-9999-9999")
            verify(accountRepository, never()).existsActiveByNameAndIdNot(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        }

        @Test
        @DisplayName("T12 name 페이로드 미포함 + 기존 entity 의 name 이 prefix 위반 - 검증 skip + 다른 필드 갱신")
        fun t12_existingPrefixViolation_skipWhenNameOmitted() {
            val account = nativeAccount(name = "강남점") // prefix 위반 상태
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(phone = "02-9999-9999")
            )

            assertThat(account.name).isEqualTo("강남점") // 보존
            assertThat(account.phone).isEqualTo("02-9999-9999")
        }
    }

    @Nested
    @DisplayName("Account 존재 검증")
    inner class AccountExistence {

        @Test
        @DisplayName("T5 비존재 id - ACCOUNT_NOT_FOUND + 메시지에 id 포함")
        fun t5_notFound() {
            whenever(accountRepository.findActiveById(eq(9999))).thenReturn(null)

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
            // findActiveById 가 is_deleted=true row 를 제외 (Repository 레벨 필터)
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(null)

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
            whenever(accountRepository.existsActiveByNameAndIdNot(eq("(신규) 다른지점"), eq(accountId)))
                .thenReturn(true)

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
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 강남점")
            )

            assertThat(account.name).isEqualTo("(신규) 강남점")
            verify(accountRepository, never()).existsActiveByNameAndIdNot(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        }

        @Test
        @DisplayName("T11 name 동일 (변경 안 함) but prefix 위반 - 페이로드에 name 포함 시 항상 발동")
        fun t11_unchangedButPrefixViolation_alwaysFires() {
            val account = nativeAccount(name = "강남점") // 기존 name 도 prefix 위반
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

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
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)
            whenever(employeeRepository.findByEmployeeCode(eq("200456")))
                .thenReturn(Optional.of(Employee(id = 20, employeeCode = "200456", name = "이몽룡", costCenterCode = "CC200")))

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(employeeCode = "200456")
            )

            assertThat(account.employeeCode).isEqualTo("200456")
            // Q-D 자동 재계산 안 함
            assertThat(account.branchCode).isEqualTo("C001")
            assertThat(account.branchName).isEqualTo("강남지점")
        }

        @Test
        @DisplayName("T14 employeeCode 변경 + 신규 Employee 미등록 - EMPLOYEE_NOT_FOUND")
        fun t14_employeeNotFound() {
            whenever(employeeRepository.findByEmployeeCode(eq("99999"))).thenReturn(Optional.empty())

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
            whenever(employeeRepository.findByEmployeeCode(eq("100200")))
                .thenReturn(Optional.of(Employee(id = 30, employeeCode = "100200", name = "삭제됨", costCenterCode = "CC300", isDeleted = true)))

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
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(employeeCode = "")
            )

            assertThat(account.employeeCode).isEqualTo("100123")
            verify(employeeRepository, never()).findByEmployeeCode(org.mockito.kotlin.any())
        }
    }

    @Nested
    @DisplayName("SAP 동기 키 silent ignore (Q-C)")
    inner class SapSyncKeyIgnore {

        @Test
        @DisplayName("T16 DTO 정의 자체에 SAP 동기 키 부재 - 페이로드 포함해도 entity 변경 0건")
        fun t16_silentIgnore() {
            val account = nativeAccount(externalKey = "SAP-OLD")
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            // DTO 정의에 external_key 가 없어 클라이언트가 보내도 Jackson 단에서 무시
            // service 단 구현 시 SAP 동기 키 필드 매핑 자체가 없음 → entity 보존 자동 보장
            service.update(
                id = accountId,
                principal = branchManagerPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 변경")
            )

            assertThat(account.externalKey).isEqualTo("SAP-OLD") // 보존
        }
    }

    @Nested
    @DisplayName("SYSTEM_ADMIN 검증 우회 (Q-A)")
    inner class SystemAdminBypass {

        @Test
        @DisplayName("T17 SYSTEM_ADMIN + name prefix 위반 - 우회 + 갱신 성공")
        fun t17_systemAdminBypassesPrefix() {
            val account = nativeAccount()
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            service.update(
                id = accountId,
                principal = systemAdminPrincipal,
                request = AdminAccountUpdateRequest(name = "강남점") // prefix 위반
            )

            assertThat(account.name).isEqualTo("강남점") // 우회로 갱신됨
        }

        @Test
        @DisplayName("T18 SYSTEM_ADMIN + name 동일명 중복 - 우회 + 갱신 성공")
        fun t18_systemAdminBypassesDuplicate() {
            val account = nativeAccount()
            whenever(accountRepository.findActiveById(eq(accountId))).thenReturn(account)

            service.update(
                id = accountId,
                principal = systemAdminPrincipal,
                request = AdminAccountUpdateRequest(name = "(신규) 다른지점")
            )

            assertThat(account.name).isEqualTo("(신규) 다른지점")
            // 중복 검증 자체가 호출되지 않음
            verify(accountRepository, never()).existsActiveByNameAndIdNot(org.mockito.kotlin.any(), org.mockito.kotlin.any())
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
