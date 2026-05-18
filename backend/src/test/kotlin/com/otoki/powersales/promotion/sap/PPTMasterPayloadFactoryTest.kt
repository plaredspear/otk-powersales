package com.otoki.powersales.promotion.sap

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@DisplayName("PPTMasterPayloadFactory — 17개 필드 매핑 (#765)")
class PPTMasterPayloadFactoryTest {

    private val factory = PPTMasterPayloadFactory()
    private val today: LocalDate = LocalDate.of(2026, 5, 18)

    @Test
    @DisplayName("기본 매핑 — 17개 키가 레거시 SF 필드 출처와 정합")
    fun build_basicMapping() {
        val emp = employee(name = "홍길동", code = "100123", status = "재직", appLogin = true, jikwee = "사원", org = "강남지점")
        val acc = account(externalKey = "AK001", statusName = "정상", type = AccountType.DISCOUNT_STORE)
        val master = master(
            id = 1L,
            name = "PM0000001",
            teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
            accountSfid = "001ABC",
            isConfirmed = true,
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 31),
            branchCode = "BR001",
            employee = emp,
            account = acc
        )

        val payload = factory.build(listOf(master), today)
        val row = payload.REQUEST.single()

        assertThat(row.Name).isEqualTo("PM0000001")
        assertThat(row.ProfessionalPromotionTeam).isEqualTo("라면세일조")
        assertThat(row.Account).isEqualTo("001ABC")
        assertThat(row.FullName).isEqualTo("홍길동")
        assertThat(row.EmployeeNumber).isEqualTo("100123")
        assertThat(row.AccountStatus).isEqualTo("정상")
        assertThat(row.AccountType).isEqualTo(AccountType.DISCOUNT_STORE.displayName)
        assertThat(row.AccountCode).isEqualTo("AK001")
        assertThat(row.StartDate).isEqualTo("2026-05-01")
        assertThat(row.EndDate).isEqualTo("2026-05-31")
        assertThat(row.CostCenterCode).isEqualTo("BR001")
        assertThat(row.BranchName).isEqualTo("강남지점")
        assertThat(row.Title).isEqualTo("사원")
        assertThat(row.Confirmed).isEqualTo("true")
        assertThat(row.YearMonth).isEqualTo("202605")
    }

    @Test
    @DisplayName("null endDate — 레거시 String.valueOf(null) 정합으로 \"null\" 문자열 출력")
    fun build_nullEndDateBecomesLiteralNullString() {
        val master = master(endDate = null)
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().EndDate).isEqualTo("null")
    }

    @Test
    @DisplayName("ValidData — isConfirmed=false → \"미확정\" (수식 분기 1)")
    fun build_validData_notConfirmed() {
        val master = master(isConfirmed = false)
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidData).isEqualTo("미확정")
    }

    @Test
    @DisplayName("ValidData — 시작일 <= TODAY <= 종료일 → \"유효\"")
    fun build_validData_valid() {
        val master = master(
            isConfirmed = true,
            startDate = today.minusDays(1),
            endDate = today.plusDays(1)
        )
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidData).isEqualTo("유효")
    }

    @Test
    @DisplayName("ValidData — 시작일 > TODAY → \"예정\"")
    fun build_validData_upcoming() {
        val master = master(
            isConfirmed = true,
            startDate = today.plusDays(1),
            endDate = today.plusDays(10)
        )
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidData).isEqualTo("예정")
    }

    @Test
    @DisplayName("ValidData — 종료일 < TODAY → \"종료\"")
    fun build_validData_ended() {
        val master = master(
            isConfirmed = true,
            startDate = today.minusDays(10),
            endDate = today.minusDays(1)
        )
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidData).isEqualTo("종료")
    }

    @Test
    @DisplayName("ValidConditionData — 직원 status=\"휴직\" → \"휴직\"")
    fun build_validConditionData_leave() {
        val emp = employee(status = "휴직", appLogin = true)
        val master = master(employee = emp)
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidConditionData).isEqualTo("휴직")
    }

    @Test
    @DisplayName("ValidConditionData — 퇴직 + endDate < TODAY → \"퇴직YYYY-MM-DD\"")
    fun build_validConditionData_resignedPast() {
        val empEnd = LocalDate.of(2026, 4, 30)
        val emp = employee(status = "퇴직", appLogin = false, endDate = empEnd)
        val master = master(employee = emp)
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidConditionData).isEqualTo("퇴직$empEnd")
    }

    @Test
    @DisplayName("ValidConditionData — 퇴직 + endDate > TODAY → \"퇴직예정YYYY-MM-DD\"")
    fun build_validConditionData_resignedFuture() {
        val empEnd = today.plusDays(10)
        val emp = employee(status = "퇴직", appLogin = false, endDate = empEnd)
        val master = master(employee = emp)
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidConditionData).isEqualTo("퇴직예정$empEnd")
    }

    @Test
    @DisplayName("ValidConditionData — 재직 (status=\"재직\" or 비퇴직 fallback) → \"재직\"")
    fun build_validConditionData_active() {
        val emp = employee(status = "재직", appLogin = true)
        val master = master(employee = emp)
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ValidConditionData).isEqualTo("재직")
    }

    @Test
    @DisplayName("YearMonth — 송신 시점 LocalDate.now() 의 yyyyMM")
    fun build_yearMonth() {
        val payload = factory.build(listOf(master()), LocalDate.of(2026, 1, 9))
        assertThat(payload.REQUEST.single().YearMonth).isEqualTo("202601")
    }

    @Test
    @DisplayName("ProfessionalPromotionTeam — enum displayName 한글 그대로")
    fun build_teamTypeDisplayName() {
        val master = master(teamType = ProfessionalPromotionTeamType.CURRY_PROMOTION)
        val payload = factory.build(listOf(master), today)
        assertThat(payload.REQUEST.single().ProfessionalPromotionTeam).isEqualTo("카레행사조")
    }

    private fun employee(
        name: String = "직원",
        code: String = "EMP",
        status: String? = "재직",
        appLogin: Boolean? = true,
        endDate: LocalDate? = null,
        jikwee: String? = "사원",
        org: String? = "지점"
    ): Employee {
        val mock: Employee = mock()
        whenever(mock.name).thenReturn(name)
        whenever(mock.employeeCode).thenReturn(code)
        whenever(mock.status).thenReturn(status)
        whenever(mock.appLoginActive).thenReturn(appLogin)
        whenever(mock.endDate).thenReturn(endDate)
        whenever(mock.jikwee).thenReturn(jikwee)
        whenever(mock.orgName).thenReturn(org)
        return mock
    }

    private fun account(
        externalKey: String? = "AK",
        statusName: String? = "정상",
        type: AccountType? = AccountType.DISCOUNT_STORE
    ): Account {
        val mock: Account = mock()
        whenever(mock.externalKey).thenReturn(externalKey)
        whenever(mock.accountStatusName).thenReturn(statusName)
        whenever(mock.accountType).thenReturn(type)
        return mock
    }

    private fun master(
        id: Long = 1L,
        name: String? = "PM0000001",
        teamType: ProfessionalPromotionTeamType = ProfessionalPromotionTeamType.RAMEN_SALE,
        accountSfid: String? = "001ABC",
        isConfirmed: Boolean = true,
        startDate: LocalDate = today.minusDays(1),
        endDate: LocalDate? = today.plusDays(1),
        branchCode: String? = "BR001",
        employee: Employee? = employee(),
        account: Account? = account()
    ): ProfessionalPromotionTeamMaster {
        val mock: ProfessionalPromotionTeamMaster = mock()
        whenever(mock.id).thenReturn(id)
        whenever(mock.name).thenReturn(name)
        whenever(mock.teamType).thenReturn(teamType)
        whenever(mock.accountSfid).thenReturn(accountSfid)
        whenever(mock.isConfirmed).thenReturn(isConfirmed)
        whenever(mock.startDate).thenReturn(startDate)
        whenever(mock.endDate).thenReturn(endDate)
        whenever(mock.branchCode).thenReturn(branchCode)
        whenever(mock.employee).thenReturn(employee)
        whenever(mock.account).thenReturn(account)
        return mock
    }
}
