package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.otoki.powersales.platform.common.enums.WorkingType
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 주문서 작성 거래처 후보의 **행사 축** 조회 테스트
 * ([PromotionEmployeeRepositoryCustom.findConfirmedAssignedAccountIdsByEmployeeAndDate]).
 *
 * 기준 2가지: 본인이 **확정된 행사사원**으로 등록 + 행사마스터 **기간이 조회일을 포함**.
 * 확정 판정은 행사 확정 시 채워지는 일정 백링크(`teamMemberScheduleId`)이며, 판정 단위는
 * 행사가 아니라 행사사원 개인이다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(QueryDslConfig::class)
@ActiveProfiles("test")
@DisplayName("행사사원 확정 담당 거래처 조회 (주문서 작성 행사 축)")
class PromotionEmployeeConfirmedAssignedAccountTest {

    @Autowired
    private lateinit var promotionEmployeeRepository: PromotionEmployeeRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val today: LocalDate = LocalDate.of(2026, 6, 15)
    private var seq = 0

    private lateinit var me: Employee
    private lateinit var other: Employee

    @BeforeEach
    fun setUp() {
        promotionEmployeeRepository.deleteAll()
        me = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP001", name = "본인"))
        other = testEntityManager.persistAndFlush(Employee(employeeCode = "EMP002", name = "다른사원"))
        testEntityManager.clear()
    }

    /** 행사 확정 시 생성되는 파생 일정 — 확정 판정 백링크(teamMemberScheduleId)의 실체. */
    private fun persistSchedule(employee: Employee = me): TeamMemberSchedule =
        testEntityManager.persistAndFlush(
            TeamMemberSchedule(employee = employee, workingDate = today, workingType = WorkingType.WORK)
        )

    private fun persistAccount(name: String): Account =
        testEntityManager.persistAndFlush(Account(name = name, externalKey = "ACC-${seq++}"))

    private fun persistPromotion(
        account: Account?,
        startDate: LocalDate? = today,
        endDate: LocalDate? = today,
        isDeleted: Boolean = false,
    ): Promotion {
        val promotion = Promotion(
            promotionNumber = "PM-${seq++}",
            startDate = startDate,
            endDate = endDate,
            isDeleted = isDeleted,
            account = account,
        )
        return testEntityManager.persistAndFlush(promotion)
    }

    /** 확정 행사사원 = 일정 백링크(teamMemberScheduleId) 보유. [confirmed] false 면 미확정. */
    private fun persistPromotionEmployee(
        promotion: Promotion,
        employee: Employee = me,
        confirmed: Boolean = true,
        isDeleted: Boolean? = false,
        scheduleDate: LocalDate? = today,
    ): PromotionEmployee = testEntityManager.persistAndFlush(
        PromotionEmployee(
            promotionId = promotion.id,
            employeeId = employee.id,
            scheduleDate = scheduleDate,
            teamMemberScheduleId = if (confirmed) persistSchedule(employee).id else null,
            isDeleted = isDeleted,
        )
    )

    private fun query(date: LocalDate = today): List<Long> = promotionEmployeeRepository
        .findConfirmedAssignedAccountIdsByEmployeeAndDate(me.id, date)

    @Test
    @DisplayName("확정 행사사원 + 오늘이 기간 안인 행사의 거래처를 중복 제거해 반환한다")
    fun returnsConfirmedAssignedAccounts() {
        val account = persistAccount("행사거래처")
        persistPromotionEmployee(persistPromotion(account))
        // 같은 거래처의 행사 2건 → distinct 검증
        persistPromotionEmployee(persistPromotion(account))
        testEntityManager.clear()

        assertThat(query()).containsExactly(account.id)
    }

    @Test
    @DisplayName("투입일이 오늘이 아니어도 행사 기간이 오늘을 포함하면 반환한다")
    fun includesWhenScheduleDateIsNotToday() {
        val account = persistAccount("3일행사거래처")
        val promotion = persistPromotion(account, startDate = today.minusDays(1), endDate = today.plusDays(1))
        // 여사원 투입일은 1일차뿐이지만 행사 기간은 오늘을 포함한다.
        persistPromotionEmployee(promotion, scheduleDate = today.minusDays(1))
        testEntityManager.clear()

        assertThat(query()).containsExactly(account.id)
    }

    @Test
    @DisplayName("미확정 행사사원(일정 백링크 없음)은 제외한다")
    fun excludesUnconfirmed() {
        persistPromotionEmployee(persistPromotion(persistAccount("미확정거래처")), confirmed = false)
        testEntityManager.clear()

        assertThat(query()).isEmpty()
    }

    @Test
    @DisplayName("같은 행사라도 본인 행사사원 row 가 미확정이면 제외한다")
    fun confirmationIsPerPromotionEmployee() {
        val promotion = persistPromotion(persistAccount("일부확정거래처"))
        persistPromotionEmployee(promotion, employee = other) // 타인은 확정
        persistPromotionEmployee(promotion, confirmed = false) // 본인은 미확정
        testEntityManager.clear()

        assertThat(query()).isEmpty()
    }

    @Test
    @DisplayName("행사 기간을 벗어난 날짜는 제외한다")
    fun excludesOutOfPeriod() {
        persistPromotionEmployee(
            persistPromotion(persistAccount("종료행사"), startDate = today.minusDays(5), endDate = today.minusDays(1))
        )
        persistPromotionEmployee(
            persistPromotion(persistAccount("예정행사"), startDate = today.plusDays(1), endDate = today.plusDays(5))
        )
        testEntityManager.clear()

        assertThat(query()).isEmpty()
    }

    @Test
    @DisplayName("행사 시작일/종료일이 NULL 이면 제외한다")
    fun excludesNullPeriod() {
        persistPromotionEmployee(persistPromotion(persistAccount("시작일없음"), startDate = null))
        persistPromotionEmployee(persistPromotion(persistAccount("종료일없음"), endDate = null))
        testEntityManager.clear()

        assertThat(query()).isEmpty()
    }

    @Test
    @DisplayName("다른 사원 / 삭제된 행사사원 / 삭제된 행사 / 거래처 미지정은 제외한다")
    fun excludesOtherEmployeeDeletedAndNoAccount() {
        persistPromotionEmployee(persistPromotion(persistAccount("타인행사")), employee = other)
        persistPromotionEmployee(persistPromotion(persistAccount("삭제행사사원")), isDeleted = true)
        persistPromotionEmployee(persistPromotion(persistAccount("삭제행사"), isDeleted = true))
        persistPromotionEmployee(persistPromotion(account = null))
        testEntityManager.clear()

        assertThat(query()).isEmpty()
    }

    @Test
    @DisplayName("행사사원 is_deleted 가 NULL 이면 미삭제로 통과한다 (SF 마이그레이션 row 정합)")
    fun treatsNullIsDeletedAsNotDeleted() {
        val account = persistAccount("NULL삭제여부거래처")
        persistPromotionEmployee(persistPromotion(account), isDeleted = null)
        testEntityManager.clear()

        assertThat(query()).containsExactly(account.id)
    }
}
