package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
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
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk(relaxed = true)
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
            displayWorkScheduleRepository,
            accountRepository,
            monthlySalesHistoryGateway,
            monthlyIntegrationScheduleRepository,
            branchCodeExpander,
            accountCategoryMasterRepository,
            employeeInputCriteriaMasterRepository,
            teamMemberScheduleSearchService,
            teamMemberCategorySearchService,
        )
    }

    private fun pair(statusCode: String?, accountType: String?, abcCode: String?, abcType: String?) =
        AccountDistributionAbcPairRow(statusCode, accountType, abcCode, abcType)

    @Test
    @DisplayName("유통형태별 종속 거래처유형 매핑 — co-occurrence 4-튜플에서 도출")
    fun dependentMapping() {
        every { accountRepository.findDistinctDistributionAbcPairs() } returns listOf(
            pair("02", "대형마트", "6111", "이마트"),
            pair("02", "대형마트", "6112", "홈플러스"),
            pair("01", "슈퍼", "5012", "슈퍼체인"),
        )

        val result = service.getFilterOptions()

        // 유통형태 "02 대형마트" 는 이마트/홈플러스 두 거래처유형에 종속.
        assertThat(result.dependentAccountTypes["02 대형마트"])
            .containsExactly("6111 이마트", "6112 홈플러스")
        // 유통형태 "01 슈퍼" 는 슈퍼체인 하나에만 종속.
        assertThat(result.dependentAccountTypes["01 슈퍼"])
            .containsExactly("5012 슈퍼체인")
    }

    @Test
    @DisplayName("전체 목록 — 정렬 + 중복 제거")
    fun fullLists() {
        every { accountRepository.findDistinctDistributionAbcPairs() } returns listOf(
            pair("02", "대형마트", "6111", "이마트"),
            pair("02", "대형마트", "6111", "이마트"), // 완전 중복
            pair("01", "슈퍼", "6111", "이마트"),     // 거래처유형 라벨은 중복, 유통형태는 상이
        )

        val result = service.getFilterOptions()

        // 유통형태 전체: 정렬(01 → 02) + 중복 제거.
        assertThat(result.distributions).containsExactly("01 슈퍼", "02 대형마트")
        // 거래처유형 전체: 라벨 중복("6111 이마트") 1건으로 제거.
        assertThat(result.accountTypes).containsExactly("6111 이마트")
    }

    @Test
    @DisplayName("한쪽 파트만 있는 경우 — 라벨 규칙(공백 join) 적용, 둘 다 blank 면 종속 매핑 제외")
    fun blankPartHandling() {
        every { accountRepository.findDistinctDistributionAbcPairs() } returns listOf(
            // 코드만 존재 → "02"
            pair("02", null, "6111", "이마트"),
            // 거래처유형 명칭만 존재 → "이마트체인"
            pair("01", "슈퍼", null, "이마트체인"),
            // 유통형태 파트가 둘 다 blank → 유통형태 라벨 null → 종속 매핑 미반영
            pair(null, "  ", "9999", "기타"),
        )

        val result = service.getFilterOptions()

        assertThat(result.distributions).contains("02", "01 슈퍼")
        assertThat(result.accountTypes).contains("6111 이마트", "이마트체인", "9999 기타")
        // 유통형태 라벨이 null 인 튜플은 종속 매핑 키에 존재하지 않는다.
        assertThat(result.dependentAccountTypes).doesNotContainKey("")
        assertThat(result.dependentAccountTypes["02"]).containsExactly("6111 이마트")
    }

    @Test
    @DisplayName("옵션 라벨은 companion 정본 규칙 그대로 — 코드/명칭 원본을 공백 join (trim 안 함)")
    fun labelUsesRawParts() {
        // companion 정본은 filter { isNotBlank } 후 원본을 join 하므로, 선행 공백이 있으면 옵션 라벨도 공백을 보존한다.
        // 검색 필터(labelExpr) 도 concat 에 원본 컬럼을 쓰도록 맞춰져 있어 이 옵션 라벨과 완전일치한다.
        every { accountRepository.findDistinctDistributionAbcPairs() } returns listOf(
            pair("02", " 대형마트", "6111", "이마트"),
        )

        val result = service.getFilterOptions()

        // "02" + " " + " 대형마트" → "02  대형마트" (공백 2개, trim 되지 않음).
        assertThat(result.distributions).containsExactly("02  대형마트")
        assertThat(result.dependentAccountTypes).containsKey("02  대형마트")
    }
}
