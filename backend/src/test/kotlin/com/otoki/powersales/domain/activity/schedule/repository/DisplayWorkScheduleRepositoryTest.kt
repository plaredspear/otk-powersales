package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.data.domain.PageRequest
import com.otoki.powersales.platform.common.config.QueryDslConfig
import com.querydsl.core.types.dsl.Expressions
import org.junit.jupiter.api.Nested
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class DisplayWorkScheduleRepositoryTest {

    @Autowired
    private lateinit var displayWorkScheduleRepository: DisplayWorkScheduleRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private lateinit var testEmployee: Employee
    private lateinit var testAccount1: Account
    private lateinit var testAccount2: Account
    private lateinit var testAccount3: Account
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        displayWorkScheduleRepository.deleteAll()
        testEntityManager.clear()

        testEmployee = testEntityManager.persistAndFlush(Employee(employeeCode = "20030117", name = "테스트사원"))
        testAccount1 = testEntityManager.persistAndFlush(Account(externalKey = "ACC001", name = "거래처1"))
        testAccount2 = testEntityManager.persistAndFlush(Account(externalKey = "ACC002", name = "거래처2"))
        testAccount3 = testEntityManager.persistAndFlush(Account(externalKey = "ACC003", name = "거래처3"))
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByEmployeeAndStartDate - 해당 날짜 스케줄이 있으면 목록 반환")
    fun findByEmployeeAndStartDate_withSchedules() {
        // Given
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDate(testEmployee.id, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.account?.id }).containsExactlyInAnyOrder(testAccount1.id, testAccount2.id)
    }

    @Test
    @DisplayName("findByEmployeeAndStartDate - 다른 날짜 스케줄만 있으면 빈 목록 반환")
    fun findByEmployeeAndStartDate_differentDate() {
        // Given
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plus(1, ChronoUnit.DAYS)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDate(testEmployee.id, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeAndAccountAndStartDate - 스케줄 조회 성공")
    fun findByEmployeeAndAccountAndStartDate_found() {
        // Given
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, typeOfWork1 = TypeOfWork1.DISPLAY))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndAccountAndStartDate(testEmployee.id, testAccount1.id, today)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.typeOfWork1).isEqualTo(TypeOfWork1.DISPLAY)
    }

    @Test
    @DisplayName("findByEmployeeAndAccountAndStartDate - 스케줄 미존재 시 null")
    fun findByEmployeeAndAccountAndStartDate_notFound() {
        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndAccountAndStartDate(testEmployee.id, 9999, today)

        // Then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByEmployeeAndStartDateBetween - 기간 내 스케줄 조회")
    fun findByEmployeeAndStartDateBetween_withinRange() {
        // Given
        val startDate = today
        val endDate = today.plus(6, ChronoUnit.DAYS)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plus(3, ChronoUnit.DAYS)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plus(6, ChronoUnit.DAYS)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.account?.id }).containsExactlyInAnyOrder(testAccount1.id, testAccount2.id, testAccount3.id)
    }

    @Test
    @DisplayName("findByEmployeeAndStartDateBetween - 기간 외 스케줄은 제외")
    fun findByEmployeeAndStartDateBetween_outsideRange() {
        // Given
        val startDate = today.plus(1, ChronoUnit.DAYS)
        val endDate = today.plus(3, ChronoUnit.DAYS)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plus(2, ChronoUnit.DAYS)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plus(5, ChronoUnit.DAYS)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].account?.id).isEqualTo(testAccount2.id)
    }

    @Test
    @DisplayName("findDistinctAccountIdsByEmployeeIdAndStartDateBetween - 월별 중복 제거 거래처 조회")
    fun findDistinctAccounts_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plus(10, ChronoUnit.DAYS)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plus(3, ChronoUnit.DAYS)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plus(7, ChronoUnit.DAYS)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plus(2, ChronoUnit.DAYS)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plus(5, ChronoUnit.DAYS)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plus(9, ChronoUnit.DAYS)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyInAnyOrder(testAccount1.id, testAccount2.id, testAccount3.id)
    }

    @Test
    @DisplayName("findDistinctAccountIdsByEmployeeIdAndStartDateBetween - 스케줄 없는 사용자는 빈 리스트 반환")
    fun findDistinctAccounts_noSchedules() {
        // Given
        val startDate = today
        val endDate = today.plus(10, ChronoUnit.DAYS)
        val otherEmployeeId = 99999L

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndStartDateBetween(otherEmployeeId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeAndStartDateBetween - 스케줄 없는 기간 조회 시 빈 리스트")
    fun findByEmployeeAndStartDateBetween_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plus(10, ChronoUnit.DAYS)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Nested
    @DisplayName("findConfirmedValidByEmployeeIdAndDateRange — 기간 겹침(확정) 진열마스터")
    inner class FindConfirmedValidByEmployeeIdAndDateRangeTests {

        @Test
        @DisplayName("시작일이 조회 기간 이전이어도 기간이 겹치면 조회됨")
        fun startBeforeRange_overlaps_returns() {
            // 기간: [1일, 말일], 마스터: startDate 전월 20일 ~ endDate 이달 10일
            val from = today.withDayOfMonth(1)
            val to = today.withDayOfMonth(today.lengthOfMonth())
            testEntityManager.persistAndFlush(
                createSapDws(employee = testEmployee, startDate = from.minusDays(10), endDate = from.plusDays(9))
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(testEmployee.id, from, to)

            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("endDate NULL(진행 중) 마스터도 조회됨")
        fun endDateNull_ongoing_returns() {
            val from = today.withDayOfMonth(1)
            val to = today.withDayOfMonth(today.lengthOfMonth())
            testEntityManager.persistAndFlush(
                createSapDws(employee = testEmployee, startDate = from.minusMonths(3), endDate = null)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(testEmployee.id, from, to)

            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("미확정(confirmed=false) 마스터는 제외")
        fun notConfirmed_excluded() {
            val from = today.withDayOfMonth(1)
            val to = today.withDayOfMonth(today.lengthOfMonth())
            testEntityManager.persistAndFlush(
                createSapDws(employee = testEmployee, startDate = today, endDate = today, confirmed = false)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(testEmployee.id, from, to)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("soft-delete(isDeleted=true) 마스터는 제외")
        fun softDeleted_excluded() {
            val from = today.withDayOfMonth(1)
            val to = today.withDayOfMonth(today.lengthOfMonth())
            testEntityManager.persistAndFlush(
                createSapDws(employee = testEmployee, startDate = today, endDate = today, isDeleted = true)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(testEmployee.id, from, to)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("기간이 겹치지 않는 마스터는 제외")
        fun noOverlap_excluded() {
            val from = today.withDayOfMonth(1)
            val to = today.withDayOfMonth(today.lengthOfMonth())
            // 기간 전체가 조회 범위 이후
            testEntityManager.persistAndFlush(
                createSapDws(employee = testEmployee, startDate = to.plusDays(5), endDate = to.plusDays(10))
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(testEmployee.id, from, to)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("다른 사원의 마스터는 제외")
        fun otherEmployee_excluded() {
            val from = today.withDayOfMonth(1)
            val to = today.withDayOfMonth(today.lengthOfMonth())
            testEntityManager.persistAndFlush(
                createSapDws(employee = testEmployee, startDate = today, endDate = today)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findConfirmedValidByEmployeeIdAndDateRange(99999L, from, to)

            assertThat(result).isEmpty()
        }
    }

    // ========== findValidForDisplayMasterSapPaged (Spec #669 Q2 재결정) ==========

    @Nested
    @DisplayName("findValidForDisplayMasterSapPaged — ValidData '유효' 동치 WHERE 절")
    inner class FindValidForDisplayMasterSapPagedTests {

        @Test
        @DisplayName("재직 사원 + 확정 + 기간 안 + 미삭제 → 조회됨")
        fun activeEmployeeInRangeConfirmed_returns() {
            val activeEmp = persistEmployee("ACT001", status = "재직")
            testEntityManager.persistAndFlush(createSapDws(employee = activeEmp))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).hasSize(1)
            assertThat(result[0].employee?.id).isEqualTo(activeEmp.id)
        }

        @Test
        @DisplayName("퇴직 + Employee.endDate >= today → 조회됨 (SF formula '유효' 분기 B)")
        fun resignedButEmployeeEndDateFuture_returns() {
            val resignedFuture = persistEmployee(
                "RES001", status = "퇴직", endDate = today.plus(1, ChronoUnit.DAYS)
            )
            testEntityManager.persistAndFlush(createSapDws(employee = resignedFuture))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).hasSize(1)
            assertThat(result[0].employee?.id).isEqualTo(resignedFuture.id)
        }

        @Test
        @DisplayName("퇴직 + Employee.endDate < today → 제외됨 ('종료' 분기)")
        fun resignedAndEmployeeEndDatePast_excluded() {
            val resignedPast = persistEmployee(
                "RES002", status = "퇴직", endDate = today.minus(1, ChronoUnit.DAYS)
            )
            testEntityManager.persistAndFlush(createSapDws(employee = resignedPast))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("휴직 + 기간 안 → 제외됨 ('예정' 분기 — SAP 미송신)")
        fun onLeave_excluded() {
            val onLeave = persistEmployee("LEA001", status = "휴직")
            testEntityManager.persistAndFlush(createSapDws(employee = onLeave))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("appLoginActive=false + Employee.endDate >= today → 조회됨")
        fun appLoginInactiveButEmployeeEndDateFuture_returns() {
            val inactive = persistEmployee(
                "INA001", status = "재직", appLoginActive = false, endDate = today.plus(7, ChronoUnit.DAYS)
            )
            testEntityManager.persistAndFlush(createSapDws(employee = inactive))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).hasSize(1)
            assertThat(result[0].employee?.id).isEqualTo(inactive.id)
        }

        @Test
        @DisplayName("기간 밖 (StartDate > today) → 제외됨")
        fun startDateFuture_excluded() {
            val activeEmp = persistEmployee("ACT002", status = "재직")
            testEntityManager.persistAndFlush(
                createSapDws(employee = activeEmp, startDate = today.plus(1, ChronoUnit.DAYS))
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("재직 + 확정 false → 제외됨")
        fun activeButNotConfirmed_excluded() {
            val activeEmp = persistEmployee("ACT003", status = "재직")
            testEntityManager.persistAndFlush(
                createSapDws(employee = activeEmp, confirmed = false)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("재직 + is_deleted=true → 제외됨")
        fun activeButDeleted_excluded() {
            val activeEmp = persistEmployee("ACT004", status = "재직")
            testEntityManager.persistAndFlush(
                createSapDws(employee = activeEmp, isDeleted = true)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("재직 + endDate IS NULL → 조회됨 (장기 확정)")
        fun activeWithNullEndDate_returns() {
            val activeEmp = persistEmployee("ACT005", status = "재직")
            testEntityManager.persistAndFlush(
                createSapDws(employee = activeEmp, endDate = null)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(today, 100, 0)

            assertThat(result).hasSize(1)
        }
    }

    // ========== findValidForLastMonthRevenuePaged (Spec #690 — Confirmed 무관 + legacy SOQL 동등) ==========

    @Nested
    @DisplayName("findValidForLastMonthRevenuePaged — legacy UpdateLastMonthRevenueBatch SOQL 동등 (Confirmed 무관)")
    inner class FindValidForLastMonthRevenuePagedTests {

        @Test
        @DisplayName("재직 + 확정 + 기간 안 → 조회됨 (SAP outbound batch 와 동일)")
        fun activeConfirmed_returns() {
            val activeEmp = persistEmployee("LMR001", status = "재직")
            testEntityManager.persistAndFlush(createSapDws(employee = activeEmp))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForLastMonthRevenuePaged(today, 100, 0)

            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("재직 + 미확정 (confirmed=false) → **조회됨** (legacy 동등 — SAP outbound batch 와 차이)")
        fun activeUnconfirmed_returns() {
            val activeEmp = persistEmployee("LMR002", status = "재직")
            testEntityManager.persistAndFlush(createSapDws(employee = activeEmp, confirmed = false))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForLastMonthRevenuePaged(today, 100, 0)

            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("퇴직 + endDate < today → 제외됨 ('종료' 분기 — ValidData != '유효')")
        fun resignedPast_excluded() {
            val resignedPast = persistEmployee(
                "LMR003", status = "퇴직", endDate = today.minus(1, ChronoUnit.DAYS)
            )
            testEntityManager.persistAndFlush(createSapDws(employee = resignedPast))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForLastMonthRevenuePaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("휴직 → 제외됨 ('예정' 분기 — ValidData != '유효')")
        fun onLeave_excluded() {
            val onLeave = persistEmployee("LMR004", status = "휴직")
            testEntityManager.persistAndFlush(createSapDws(employee = onLeave))
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForLastMonthRevenuePaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("재직 + is_deleted=true → 제외됨")
        fun deleted_excluded() {
            val activeEmp = persistEmployee("LMR005", status = "재직")
            testEntityManager.persistAndFlush(
                createSapDws(employee = activeEmp, isDeleted = true)
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForLastMonthRevenuePaged(today, 100, 0)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("기간 밖 (startDate > today) → 제외됨")
        fun startDateFuture_excluded() {
            val activeEmp = persistEmployee("LMR006", status = "재직")
            testEntityManager.persistAndFlush(
                createSapDws(employee = activeEmp, startDate = today.plus(1, ChronoUnit.DAYS))
            )
            testEntityManager.clear()

            val result = displayWorkScheduleRepository.findValidForLastMonthRevenuePaged(today, 100, 0)

            assertThat(result).isEmpty()
        }
    }

    // ========== updateLastMonthRevenueById (Spec #690 — native UPDATE + updated_at 무영향) ==========

    @Nested
    @DisplayName("updateLastMonthRevenueById — native UPDATE")
    inner class UpdateLastMonthRevenueByIdTests {

        @Test
        @DisplayName("last_month_revenue 컬럼만 변경 + updated_at 자동 갱신 무영향 (legacy trigger bypass 의미 정합)")
        fun updates_lastMonthRevenue_without_touching_updatedAt() {
            val activeEmp = persistEmployee("LMR_UPD001", status = "재직")
            val persisted = testEntityManager.persistAndFlush(createSapDws(employee = activeEmp))
            val originalId = persisted.id
            testEntityManager.clear()

            // baseline 은 메모리 LocalDateTime (nanosecond) 이 아니라 DB reload 값 (정밀도 일치) 으로 확보
            val baselineUpdatedAt = displayWorkScheduleRepository.findById(originalId).orElseThrow().updatedAt
            testEntityManager.clear()

            // updated_at 차이 검증을 위한 시간 여유 — 짧은 sleep
            Thread.sleep(10)

            val affected = displayWorkScheduleRepository.updateLastMonthRevenueById(
                originalId, BigDecimal("12345")
            )

            testEntityManager.clear()
            val reloaded = displayWorkScheduleRepository.findById(originalId).orElseThrow()
            assertThat(affected).isEqualTo(1L)
            assertThat(reloaded.lastMonthRevenue).isEqualByComparingTo(BigDecimal("12345"))
            // updated_at 은 native UPDATE 의 영향을 받지 않음 — JPA Auditing 미개입
            assertThat(reloaded.updatedAt).isEqualTo(baselineUpdatedAt)
        }

        @Test
        @DisplayName("id 부재 → 0 row 갱신")
        fun missingId_returnsZero() {
            val affected = displayWorkScheduleRepository.updateLastMonthRevenueById(
                999_999L, BigDecimal("1")
            )

            assertThat(affected).isEqualTo(0L)
        }
    }

    // ========== Helpers ==========

    private fun persistEmployee(
        employeeCode: String,
        status: String,
        appLoginActive: Boolean? = null,
        endDate: LocalDate? = null
    ): Employee {
        val employee = Employee(employeeCode = employeeCode, name = "테스트-$employeeCode").apply {
            this.status = status
            this.appLoginActive = appLoginActive
            this.endDate = endDate
        }
        return testEntityManager.persistAndFlush(employee)
    }

    private fun createSapDws(
        employee: Employee,
        account: Account = testAccount1,
        startDate: LocalDate = today,
        endDate: LocalDate? = today,
        confirmed: Boolean = true,
        isDeleted: Boolean? = null
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            employee = employee,
            account = account,
            startDate = startDate,
            endDate = endDate,
            typeOfWork1 = TypeOfWork1.DISPLAY,
        ).apply {
            this.confirmed = confirmed
            this.isDeleted = isDeleted
        }
    }

    private fun createDisplayWorkSchedule(
        account: Account = testAccount1,
        typeOfWork1: TypeOfWork1? = TypeOfWork1.DISPLAY,
        startDate: LocalDate = today
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            employee = testEmployee,
            account = account,
            typeOfWork1 = typeOfWork1,
            startDate = startDate
        )
    }

    @Nested
    @DisplayName("findScheduleList - 사원 검색(사번+성명 겸용) 필터")
    inner class FindScheduleListEmployeeSearch {

        private fun scheduleFor(employee: Employee): DisplayWorkSchedule =
            DisplayWorkSchedule(
                employee = employee,
                account = testAccount1,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = today,
            )

        private fun search(keyword: String?): List<String?> =
            displayWorkScheduleRepository.findScheduleList(
                keyword, null, null, null, null, null, null, null, null,
                Expressions.TRUE,
                PageRequest.of(0, 50),
            ).content.map { it.employeeName }

        @Test
        @DisplayName("사번 부분 일치로 조회")
        fun matchByEmployeeCode() {
            val emp = testEntityManager.persistAndFlush(Employee(employeeCode = "L0250014", name = "조시영"))
            testEntityManager.persistAndFlush(scheduleFor(emp))
            testEntityManager.clear()

            assertThat(search("0250014")).containsExactly("조시영")
        }

        @Test
        @DisplayName("성명 부분 일치로 조회")
        fun matchByName() {
            val emp = testEntityManager.persistAndFlush(Employee(employeeCode = "L0250014", name = "조시영"))
            testEntityManager.persistAndFlush(scheduleFor(emp))
            testEntityManager.clear()

            assertThat(search("시영")).containsExactly("조시영")
        }

        @Test
        @DisplayName("사번·성명 어느 쪽에도 없으면 제외")
        fun noMatch() {
            val emp = testEntityManager.persistAndFlush(Employee(employeeCode = "L0250014", name = "조시영"))
            testEntityManager.persistAndFlush(scheduleFor(emp))
            testEntityManager.clear()

            assertThat(search("없는사람")).isEmpty()
        }
    }

    @Nested
    @DisplayName("findScheduleList - 지점 필터(branchCodes) 는 owner 소속 지점 기준")
    inner class FindScheduleListBranchScope {

        private fun ownerWithCostCenter(costCenterCode: String?): User =
            testEntityManager.persistAndFlush(
                User(username = "owner-$costCenterCode-${System.nanoTime()}", employeeCode = null, password = "x", costCenterCode = costCenterCode)
            )

        private fun scheduleOwnedBy(
            employee: Employee,
            owner: User?,
            scheduleCostCenterCode: String?,
        ): DisplayWorkSchedule =
            DisplayWorkSchedule(
                employee = employee,
                account = testAccount1,
                typeOfWork1 = TypeOfWork1.DISPLAY,
                startDate = today,
                costCenterCode = scheduleCostCenterCode,
                ownerUser = owner,
            )

        private fun searchByBranch(branchCodes: List<String>?): List<String?> =
            displayWorkScheduleRepository.findScheduleList(
                null, null, null, null, null, null, null, null, branchCodes,
                Expressions.TRUE,
                PageRequest.of(0, 50),
            ).content.map { it.employeeName }

        @Test
        @DisplayName("스케줄 costCenterCode 가 옛 지점(5453)이라도 owner 소속(5816)이 필터에 포함되면 조회됨")
        fun matchByOwnerBranchNotScheduleSnapshot() {
            // 사원 전출 후: 스케줄 costCenterCode 는 저장 시점 스냅샷(5453)으로 고정,
            // owner 는 현재 조직 조장(5816) 으로 재계산된 상태 (홍유미/임연숙 운영 케이스 재현).
            val emp = testEntityManager.persistAndFlush(Employee(employeeCode = "20210283", name = "홍유미"))
            val owner = ownerWithCostCenter("5816")
            testEntityManager.persistAndFlush(scheduleOwnedBy(emp, owner, scheduleCostCenterCode = "5453"))
            testEntityManager.clear()

            // 지점 5816 으로 필터 → owner 소속이 5816 이므로 조회되어야 함 (스케줄 스냅샷 5453 무관).
            assertThat(searchByBranch(listOf("5816"))).containsExactly("홍유미")
        }

        @Test
        @DisplayName("owner 소속 지점이 필터 밖이면 제외 (스케줄 costCenterCode 가 필터에 있어도)")
        fun excludeWhenOwnerBranchOutsideEvenIfScheduleSnapshotInside() {
            val emp = testEntityManager.persistAndFlush(Employee(employeeCode = "20210283", name = "홍유미"))
            val owner = ownerWithCostCenter("9999")
            // 스케줄 스냅샷은 필터(5816)에 들어가지만 owner 소속(9999)은 밖 → 제외되어야 함.
            testEntityManager.persistAndFlush(scheduleOwnedBy(emp, owner, scheduleCostCenterCode = "5816"))
            testEntityManager.clear()

            assertThat(searchByBranch(listOf("5816"))).isEmpty()
        }

        @Test
        @DisplayName("owner 가 없으면(null) 지점 필터 적용 시 제외")
        fun excludeWhenOwnerNull() {
            val emp = testEntityManager.persistAndFlush(Employee(employeeCode = "20210283", name = "홍유미"))
            testEntityManager.persistAndFlush(scheduleOwnedBy(emp, owner = null, scheduleCostCenterCode = "5816"))
            testEntityManager.clear()

            assertThat(searchByBranch(listOf("5816"))).isEmpty()
        }

        @Test
        @DisplayName("branchCodes=null 이면 지점 필터 미적용 (owner 무관 전건)")
        fun noBranchFilterReturnsAll() {
            val emp = testEntityManager.persistAndFlush(Employee(employeeCode = "20210283", name = "홍유미"))
            val owner = ownerWithCostCenter("5816")
            testEntityManager.persistAndFlush(scheduleOwnedBy(emp, owner, scheduleCostCenterCode = "5453"))
            testEntityManager.clear()

            assertThat(searchByBranch(null)).containsExactly("홍유미")
        }
    }
}
