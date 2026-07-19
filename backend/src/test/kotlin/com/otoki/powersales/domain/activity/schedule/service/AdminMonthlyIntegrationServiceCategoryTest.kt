package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberCategoryResultItem
import com.otoki.powersales.domain.activity.schedule.dto.response.TeamMemberCategorySearchResult
import com.otoki.powersales.domain.activity.schedule.repository.EmployeeInputCriteriaMasterRepository
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountCategoryMasterRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * getCategorySchedule — SF `CategorySearchByTeamMemberController` 정합.
 * 특히 setNull (양월 0 지점) 행 보존: SF 는 행을 유지한 채 수치만 null 로 비우며
 * Aura 도 무필터 바인딩이라 지점명 + 빈 칸 행이 표시된다.
 */
@DisplayName("AdminMonthlyIntegrationService.getCategorySchedule — SF setNull 행 보존 정합")
class AdminMonthlyIntegrationServiceCategoryTest {

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
        )
    }

    private fun principal() = WebUserPrincipal(
        userId = 1L,
        usernameValue = "u@otokims.co.kr",
        employeeCode = "S001",
        employeeId = 1L,
        role = null,
        costCenterCode = "5832",
        isSalesSupport = false,
        passwordChangeRequired = false,
        permissions = emptySet(),
        encodedPassword = "enc",
        grantedAuthorities = emptyList(),
        active = true,
    )

    @Test
    @DisplayName("양월 합계 0 지점 (setNull 행) 도 응답에서 제거하지 않고 수치 null 로 유지")
    fun allZeroBranchRowRetainedWithNulls() {
        every { teamMemberCategorySearchService.search("2026", "6", listOf("5832", "5833"), any()) } returns
            TeamMemberCategorySearchResult(
                resultCode = "S",
                resultMsg = null,
                result = listOf(
                    // setNull 상태 — branchName 외 전부 null (SF cls:341-363 동등)
                    TeamMemberCategoryResultItem(branchName = "강북4지점"),
                    TeamMemberCategoryResultItem(
                        branchName = "원주1지점",
                        fix = BigDecimal("1.000"),
                        currentExhibitionTotal = BigDecimal("1.000"),
                        currentMonthTotal = BigDecimal("1.0"),
                        lastMonthTotal = BigDecimal("0.0"),
                        totalIncrease = BigDecimal("1.0"),
                    ),
                ),
            )

        val response = service.getCategorySchedule(2026, 6, listOf("5832", "5833"), principal())

        assertThat(response.items).hasSize(2)
        val zeroRow = response.items.first { it.branchName == "강북4지점" }
        assertThat(zeroRow.currentMonthTotal).isNull()
        assertThat(zeroRow.displayFixed).isNull()
        assertThat(zeroRow.eventAmbient).isNull()
        val activeRow = response.items.first { it.branchName == "원주1지점" }
        assertThat(activeRow.currentMonthTotal).isEqualByComparingTo(BigDecimal("1.0"))
        assertThat(activeRow.displayFixed).isEqualByComparingTo(BigDecimal("1.000"))
    }
}
