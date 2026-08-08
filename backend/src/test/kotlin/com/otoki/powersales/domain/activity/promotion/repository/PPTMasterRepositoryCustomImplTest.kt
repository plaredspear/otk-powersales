package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("PPTMasterRepositoryCustomImpl - findValidMasters 유효 조건 (legacy ValidData__c='유효' 정합)")
class PPTMasterRepositoryCustomImplTest {

    @Autowired private lateinit var repository: PPTMasterRepository
    @Autowired private lateinit var em: TestEntityManager

    private val today: LocalDate = LocalDate.of(2026, 6, 15)

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        em.clear()
    }

    // employeeId 는 EMPLOYEE FK 제약이 있어 본 테스트(유효 조건만 검증)에선 null 로 둔다.
    private fun persist(
        startDate: LocalDate,
        endDate: LocalDate?,
        isConfirmed: Boolean,
    ): ProfessionalPromotionTeamMaster {
        val master = ProfessionalPromotionTeamMaster(
            employeeId = null,
            teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
            startDate = startDate,
            endDate = endDate,
            isConfirmed = isConfirmed,
        )
        em.persistAndFlush(master)
        return master
    }

    @Test
    @DisplayName("미확정(isConfirmed=false) 마스터는 날짜가 유효해도 제외 — legacy ValidData__c formula Confirmed==false→'미확정' 동등")
    fun findValidMasters_excludesUnconfirmed() {
        // 날짜는 유효 범위(시작 ≤ today ≤ 종료) 인데 미확정인 마스터
        val unconfirmed = persist(
            startDate = today.minusDays(1),
            endDate = today.plusDays(10),
            isConfirmed = false,
        )
        // 동일 날짜 + 확정 마스터
        val confirmed = persist(
            startDate = today.minusDays(1),
            endDate = today.plusDays(10),
            isConfirmed = true,
        )
        em.clear()

        val result = repository.findValidMasters(today)

        val ids = result.map { it.id }
        assertThat(ids).contains(confirmed.id)
        assertThat(ids).doesNotContain(unconfirmed.id)
    }

    @Test
    @DisplayName("확정 + 날짜 유효 — 종료일 NULL / 종료일 미래 / 종료일=today 모두 포함")
    fun findValidMasters_includesConfirmedWithinDateRange() {
        val endNull = persist(today.minusDays(5), null, isConfirmed = true)
        val endFuture = persist(today.minusDays(5), today.plusDays(5), isConfirmed = true)
        val endToday = persist(today.minusDays(5), today, isConfirmed = true)
        em.clear()

        val result = repository.findValidMasters(today)

        assertThat(result.map { it.id })
            .containsExactlyInAnyOrder(endNull.id, endFuture.id, endToday.id)
    }

    @Test
    @DisplayName("날짜 범위 밖(시작 미래 / 종료 과거)은 확정이어도 제외")
    fun findValidMasters_excludesOutOfDateRange() {
        val notStarted = persist(today.plusDays(1), null, isConfirmed = true)
        val alreadyEnded = persist(today.minusDays(10), today.minusDays(1), isConfirmed = true)
        em.clear()

        val result = repository.findValidMasters(today)

        assertThat(result.map { it.id })
            .doesNotContain(notStarted.id, alreadyEnded.id)
    }

    @Test
    @DisplayName("findExpiringMasters - 종료일=today 라도 미확정(isConfirmed=false)은 제외, 확정만 포함 (legacy Batch_PPTMaster2 정합)")
    fun findExpiringMasters_excludesUnconfirmed() {
        // 종료일이 today 인 미확정 마스터 — 레거시 ValidData__c='유효' 전제로 만료 대상에서 제외
        val unconfirmed = persist(today.minusDays(3), today, isConfirmed = false)
        // 종료일이 today 인 확정 마스터 — 만료 대상
        val confirmed = persist(today.minusDays(3), today, isConfirmed = true)
        // 종료일이 today 가 아닌 확정 마스터 — 만료 대상 아님
        val notExpiringToday = persist(today.minusDays(3), today.plusDays(1), isConfirmed = true)
        em.clear()

        val result = repository.findExpiringMasters(today)

        val ids = result.map { it.id }
        assertThat(ids).contains(confirmed.id)
        assertThat(ids).doesNotContain(unconfirmed.id, notExpiringToday.id)
    }

    @Test
    @DisplayName("searchMasters validOnly=true - 미확정(isConfirmed=false)은 날짜가 유효해도 제외 — legacy ValidData__c='유효' 정합")
    fun searchMasters_validOnly_excludesUnconfirmed() {
        // 날짜는 유효 범위인데 미확정 — SF '유효만' 에서는 미확정이 보이지 않아야 한다
        val unconfirmed = persist(today.minusDays(1), today.plusDays(10), isConfirmed = false)
        val confirmed = persist(today.minusDays(1), today.plusDays(10), isConfirmed = true)
        em.clear()

        val result = repository.searchMasters(
            employeeName = null,
            employeeCode = null,
            teamType = null,
            branchCodeFilter = null,
            validOnly = true,
            employmentStatus = null,
            today = today,
            pageable = PageRequest.of(0, 20),
        )

        val ids = result.content.map { it.master.id }
        assertThat(ids).contains(confirmed.id)
        assertThat(ids).doesNotContain(unconfirmed.id)
    }

    private fun persistType(teamType: ProfessionalPromotionTeamType): ProfessionalPromotionTeamMaster {
        val master = ProfessionalPromotionTeamMaster(
            employeeId = null,
            teamType = teamType,
            startDate = today.minusDays(1),
            endDate = today.plusDays(10),
            isConfirmed = true,
        )
        em.persistAndFlush(master)
        return master
    }

    @Test
    @DisplayName("searchMasters 정렬 - 전문행사조 유형(SF picklist 정의 순서) 우선, 가나다순 아님")
    fun searchMasters_orderByTeamTypeDefinitionOrder() {
        // 입력 순서를 정의 순서와 다르게 (카레 → 라면 → 프레시만두) 넣어, 결과가 정의 순서로 재정렬되는지 확인.
        persistType(ProfessionalPromotionTeamType.CURRY_PROMOTION)
        persistType(ProfessionalPromotionTeamType.RAMEN_SALE)
        persistType(ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING)
        persistType(ProfessionalPromotionTeamType.FRESH_SALE_FROZEN)
        em.clear()

        val result = repository.searchMasters(
            employeeName = null,
            employeeCode = null,
            teamType = null,
            branchCodeFilter = null,
            validOnly = false,
            employmentStatus = null,
            today = today,
            pageable = PageRequest.of(0, 20),
        )

        // enum 선언 순서: 라면 → 프레시_냉동 → 프레시_냉장 → 프레시_만두 → 카레
        assertThat(result.content.map { it.master.teamType }).containsExactly(
            ProfessionalPromotionTeamType.RAMEN_SALE,
            ProfessionalPromotionTeamType.FRESH_SALE_FROZEN,
            ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING,
            ProfessionalPromotionTeamType.CURRY_PROMOTION,
        )
    }

    @Test
    @DisplayName("searchMasters validOnly=false - 미확정/날짜범위 밖 마스터도 모두 포함")
    fun searchMasters_validOnlyFalse_includesAll() {
        val unconfirmed = persist(today.minusDays(1), today.plusDays(10), isConfirmed = false)
        val ended = persist(today.minusDays(10), today.minusDays(1), isConfirmed = true)
        val valid = persist(today.minusDays(1), today.plusDays(10), isConfirmed = true)
        em.clear()

        val result = repository.searchMasters(
            employeeName = null,
            employeeCode = null,
            teamType = null,
            branchCodeFilter = null,
            validOnly = false,
            employmentStatus = null,
            today = today,
            pageable = PageRequest.of(0, 20),
        )

        assertThat(result.content.map { it.master.id })
            .contains(unconfirmed.id, ended.id, valid.id)
    }

    private fun persistEmployee(status: String?, ordDetailNode: String? = null): Employee =
        em.persistAndFlush(
            Employee(employeeCode = null, name = "테스트여사원", status = status, ordDetailNode = ordDetailNode),
        )

    private fun persistForEmployee(employeeId: Long): ProfessionalPromotionTeamMaster =
        em.persistAndFlush(
            ProfessionalPromotionTeamMaster(
                employeeId = employeeId,
                teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
                startDate = today.minusDays(1),
                endDate = today.plusDays(10),
                isConfirmed = true,
            ),
        )

    private fun searchByEmploymentStatus(status: String?) = repository.searchMasters(
        employeeName = null,
        employeeCode = null,
        teamType = null,
        branchCodeFilter = null,
        validOnly = false,
        employmentStatus = status,
        today = today,
        pageable = PageRequest.of(0, 20),
    )

    @Test
    @DisplayName("searchMasters 재직상태 - 여사원 현황과 동일 축(사원 원본 status 매칭), 미지정이면 전체")
    fun searchMasters_employmentStatus_matchesEmployeeStatus() {
        val active = persistForEmployee(persistEmployee("재직").id)
        val onLeave = persistForEmployee(persistEmployee("휴직").id)
        val resigned = persistForEmployee(persistEmployee("퇴직").id)
        // 사원 미배정 마스터 — 재직상태 조회 대상이 없어 어떤 상태로도 잡히지 않는다.
        val noEmployee = persist(today.minusDays(1), today.plusDays(10), isConfirmed = true)
        em.clear()

        assertThat(searchByEmploymentStatus("재직").content.map { it.master.id })
            .containsExactly(active.id)
        assertThat(searchByEmploymentStatus("휴직").content.map { it.master.id })
            .containsExactly(onLeave.id)
        assertThat(searchByEmploymentStatus("퇴직").content.map { it.master.id })
            .containsExactly(resigned.id)
        assertThat(searchByEmploymentStatus(null).content.map { it.master.id })
            .contains(active.id, onLeave.id, resigned.id, noEmployee.id)
    }

    @Test
    @DisplayName("searchMasters 재직상태 - 발령명 '면직' 은 퇴직 취급 (퇴직 조회에 포함 / 재직 조회에서 제외) — 여사원 현황 정합")
    fun searchMasters_employmentStatus_treatsDismissalAsResigned() {
        // status 가 아직 '재직' 으로 남은 면직자 (SAP 상태 갱신 누락) — DismissalPolicy 정합
        val dismissed = persistForEmployee(persistEmployee("재직", ordDetailNode = "면직").id)
        val active = persistForEmployee(persistEmployee("재직").id)
        em.clear()

        assertThat(searchByEmploymentStatus("재직").content.map { it.master.id })
            .containsExactly(active.id)
        assertThat(searchByEmploymentStatus("퇴직").content.map { it.master.id })
            .containsExactly(dismissed.id)
    }

    // --- findLegacyDuplicateMasters (legacy ChangeToNormal dup SOQL 정합) ---

    private fun persistAccount(externalKey: String): Account =
        em.persistAndFlush(Account(name = "테스트거래처-$externalKey", externalKey = externalKey))

    private fun persistDup(
        employeeId: Long,
        accountId: Long,
        teamType: ProfessionalPromotionTeamType,
        startDate: LocalDate,
        endDate: LocalDate?,
        isConfirmed: Boolean = true,
    ): ProfessionalPromotionTeamMaster =
        em.persistAndFlush(
            ProfessionalPromotionTeamMaster(
                employeeId = employeeId,
                accountId = accountId,
                teamType = teamType,
                startDate = startDate,
                endDate = endDate,
                isConfirmed = isConfirmed,
            ),
        )

    private fun findDup(
        employeeId: Long,
        accountId: Long,
        newStartDate: LocalDate,
        excludeId: Long? = null,
        teamType: ProfessionalPromotionTeamType = ProfessionalPromotionTeamType.RAMEN_SALE,
    ) = repository.findLegacyDuplicateMasters(
        employeeId = employeeId,
        accountId = accountId,
        teamType = teamType,
        newStartDate = newStartDate,
        today = today,
        excludeId = excludeId,
    )

    @Test
    @DisplayName("findLegacyDuplicateMasters - 종료일 없는 동일 거래처+동일 조 마스터는 중복이 아니다 (legacy `EndDate__c <= :StartDate__c` 는 null 미매칭)")
    fun findLegacyDuplicateMasters_excludesOpenEndedMaster() {
        val emp = persistEmployee("재직")
        val acc = persistAccount("DUP001")
        // 확정 + 시작일 도래 + 종료일 없음 = 현재 진행 중인 동일 거래처/동일 조 마스터
        persistDup(emp.id, acc.id, ProfessionalPromotionTeamType.RAMEN_SALE, today.minusDays(10), null)
        em.clear()

        assertThat(findDup(emp.id, acc.id, newStartDate = today.plusDays(1))).isEmpty()
    }

    @Test
    @DisplayName("findLegacyDuplicateMasters - 종료일이 today ~ 신규 시작일 구간이면 중복 (legacy 매칭 구간)")
    fun findLegacyDuplicateMasters_matchesEndDateWithinWindow() {
        val emp = persistEmployee("재직")
        val acc = persistAccount("DUP002")
        val inWindow = persistDup(
            emp.id, acc.id, ProfessionalPromotionTeamType.RAMEN_SALE, today.minusDays(10), today.plusDays(3),
        )
        em.clear()

        assertThat(findDup(emp.id, acc.id, newStartDate = today.plusDays(5)).map { it.id })
            .containsExactly(inWindow.id)
    }

    @Test
    @DisplayName("findLegacyDuplicateMasters - 종료일이 신규 시작일보다 뒤면 중복 아님 (기간이 겹쳐도 통과)")
    fun findLegacyDuplicateMasters_excludesEndDateAfterNewStart() {
        val emp = persistEmployee("재직")
        val acc = persistAccount("DUP003")
        persistDup(emp.id, acc.id, ProfessionalPromotionTeamType.RAMEN_SALE, today.minusDays(10), today.plusDays(30))
        em.clear()

        assertThat(findDup(emp.id, acc.id, newStartDate = today.plusDays(5))).isEmpty()
    }

    @Test
    @DisplayName("findLegacyDuplicateMasters - 미확정 / 시작일 미도래 / 다른 거래처 / 다른 조는 모두 제외 (ValidData__c='유효' + 거래처·조 일치 조건)")
    fun findLegacyDuplicateMasters_excludesNonValidAndOtherKeys() {
        val emp = persistEmployee("재직")
        val acc = persistAccount("DUP004")
        val otherAcc = persistAccount("DUP005")
        val newStart = today.plusDays(5)
        // 종료일은 매칭 구간이지만 각각 미확정 / 예정(시작일 미도래) / 다른 거래처 / 다른 조
        persistDup(emp.id, acc.id, ProfessionalPromotionTeamType.RAMEN_SALE, today.minusDays(10), today.plusDays(3), isConfirmed = false)
        persistDup(emp.id, acc.id, ProfessionalPromotionTeamType.RAMEN_SALE, today.plusDays(1), today.plusDays(3))
        persistDup(emp.id, otherAcc.id, ProfessionalPromotionTeamType.RAMEN_SALE, today.minusDays(10), today.plusDays(3))
        persistDup(emp.id, acc.id, ProfessionalPromotionTeamType.CURRY_PROMOTION, today.minusDays(10), today.plusDays(3))
        em.clear()

        assertThat(findDup(emp.id, acc.id, newStartDate = newStart)).isEmpty()
    }

    @Test
    @DisplayName("findLegacyDuplicateMasters - excludeId(자기 자신)는 제외 (legacy `Id != :obj.Id`)")
    fun findLegacyDuplicateMasters_excludesSelf() {
        val emp = persistEmployee("재직")
        val acc = persistAccount("DUP006")
        val self = persistDup(
            emp.id, acc.id, ProfessionalPromotionTeamType.RAMEN_SALE, today.minusDays(10), today.plusDays(3),
        )
        em.clear()

        assertThat(findDup(emp.id, acc.id, newStartDate = today.plusDays(5))).isNotEmpty()
        assertThat(findDup(emp.id, acc.id, newStartDate = today.plusDays(5), excludeId = self.id)).isEmpty()
    }
}
