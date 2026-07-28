package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.EmployeeInputCriteriaMasterRepository
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountDistributionAbcPairRow
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.domain.foundation.account.service.AccountCategoryLookupFixture
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * `AdminMonthlyIntegrationService.getFilterOptions` — 통합일정 조회조건 드롭다운 옵션 조립 단위 테스트.
 *
 * 검증 축: (유통형태, 거래처유형) 동시출현 distinct 4-튜플에서 라벨 조합 규칙(companion 정본) 재사용,
 * 유통형태별 종속 거래처유형 매핑, 전체 목록 정렬/중복제거, blank/null 파트 처리.
 */
@DisplayName("AdminMonthlyIntegrationService.getFilterOptions — 유통형태/거래처유형 종속 옵션")
class AdminMonthlyIntegrationServiceFilterOptionsTest {

    private val organizationRepository: OrganizationRepository = mockk(relaxed = true)
    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk(relaxed = true)
    private val monthlyIntegrationScheduleRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository =
        mockk(relaxed = true)
    private val branchCodeExpander: BranchCodeExpander = mockk(relaxed = true)
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository = mockk(relaxed = true)
    private val employeeInputCriteriaMasterRepository: EmployeeInputCriteriaMasterRepository = mockk(relaxed = true)
    private val teamMemberScheduleSearchService: TeamMemberScheduleSearchService = mockk(relaxed = true)
    private val teamMemberCategorySearchService: TeamMemberCategorySearchService = mockk(relaxed = true)

    private lateinit var service: AdminMonthlyIntegrationService

    @BeforeEach
    fun setUp() {
        service = AdminMonthlyIntegrationService(
            organizationRepository,
            employeeRepository,
            teamMemberScheduleRepository,
            accountRepository,
            monthlySalesHistoryGateway,
            monthlyIntegrationScheduleRepository,
            branchCodeExpander,
            accountCategoryMasterRepository,
            employeeInputCriteriaMasterRepository,
            teamMemberScheduleSearchService,
            teamMemberCategorySearchService,
            AccountCategoryLookupFixture.lookup(),
        )
    }

    private fun pair(statusCode: String?, accountType: String?, abcCode: String?, abcType: String?) =
        AccountDistributionAbcPairRow(statusCode, accountType, abcCode, abcType)

    @Test
    @DisplayName("유통형태별 종속 거래처유형 매핑 — key 는 거래처유형코드")
    fun dependentMapping() {
        // 거래처상태코드(첫 파트)는 유통형태와 무관한 축이라 매핑에 영향이 없어야 한다 — 일부러 뒤섞어 둔다.
        every { accountRepository.findDistinctDistributionAbcPairs() } returns listOf(
            pair("02", "대형마트(3대)", "6111", "이마트"),
            pair("03", "대형마트(3대)", "6112", "홈플러스"),
            pair("01", "슈퍼", "5012", "슈퍼체인"),
        )

        val result = service.getFilterOptions()

        // 코드 01 = 대형마트(3대) — 상태코드가 02/03 으로 갈려도 한 key 로 모인다.
        assertThat(result.dependentAccountTypes["01"])
            .containsExactly("6111 이마트", "6112 홈플러스")
        // 코드 06 = 슈퍼.
        assertThat(result.dependentAccountTypes["06"])
            .containsExactly("5012 슈퍼체인")
    }

    @Test
    @DisplayName("유통형태 목록은 거래처유형마스터 전량(코드 오름차순), 거래처유형은 정렬 + 중복 제거")
    fun fullLists() {
        every { accountRepository.findDistinctDistributionAbcPairs() } returns listOf(
            pair("02", "대형마트(3대)", "6111", "이마트"),
            pair("02", "대형마트(3대)", "6111", "이마트"), // 완전 중복
            pair("01", "슈퍼", "6111", "이마트"),
        )

        val result = service.getFilterOptions()

        // 유통형태 전체 = 마스터 전량. Account 에 유형이 없어도 옵션은 마스터가 정본.
        assertThat(result.distributions.map { it.code })
            .containsExactlyElementsOf(AccountCategoryLookupFixture.MASTER.map { it.first })
        assertThat(result.distributions.first().label).isEqualTo("01 대형마트(3대)")
        // 거래처유형 전체: 라벨 중복("6111 이마트") 1건으로 제거.
        assertThat(result.accountTypes).containsExactly("6111 이마트")
    }

    @Test
    @DisplayName("유형 미지정/마스터 미등록 거래처는 종속 매핑에서 제외 (거래처유형 목록에는 포함)")
    fun unmappedTypeHandling() {
        every { accountRepository.findDistinctDistributionAbcPairs() } returns listOf(
            pair("02", null, "6111", "이마트"),          // 유형 미지정
            pair("01", "  ", "9999", "기타"),            // 공백 유형
            pair("01", "마스터에없는유형", "5012", "슈퍼체인"), // 마스터 미등록
            pair("02", "슈퍼", null, "이마트체인"),
        )

        val result = service.getFilterOptions()

        // 거래처유형(ABC) 축은 Account 실재값이라 그대로 노출된다.
        assertThat(result.accountTypes).contains("6111 이마트", "9999 기타", "5012 슈퍼체인", "이마트체인")
        // 코드로 환원되지 않는 유형은 종속 매핑 key 가 될 수 없다.
        assertThat(result.dependentAccountTypes.keys).containsExactly("06")
        assertThat(result.dependentAccountTypes["06"]).containsExactly("이마트체인")
    }
}
