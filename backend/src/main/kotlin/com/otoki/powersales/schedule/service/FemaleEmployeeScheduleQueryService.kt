package com.otoki.powersales.schedule.service

import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.schedule.dto.response.FemaleEmployeeScheduleEventDto
import com.otoki.powersales.schedule.dto.response.FemaleEmployeeScheduleSummaryDto
import com.otoki.powersales.schedule.entity.QTeamMemberSchedule.Companion.teamMemberSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 여사원 일정 캘린더 조회 서비스.
 *
 * SF `FullCalendarComponentController` (`fetchAllShcedule` + `fetchScheduleSummary`) 의 backend 이식.
 * BranchMapping 확장 비대칭 (D1=b) + 하드코딩 4명 사번 분기 (D2=(i), D2-a 메소드별 코드셋) 정책 유지.
 */
@Service
@Transactional(readOnly = true)
class FemaleEmployeeScheduleQueryService(
    private val expander: BranchCodeExpander,
    private val employeeRepository: EmployeeRepository,
    private val queryFactory: JPAQueryFactory,
) {

    /**
     * 캘린더 이벤트 조회 (SF `fetchAllShcedule`).
     *
     * `accountIds` 또는 `teamMemberIds` 중 하나의 분기로 일정 조회.
     * BranchMapping 확장 적용 — 단, 하드코딩 사번이면 [FETCH_ALL_HARDCODED_CODES] 사용.
     */
    fun fetchAll(
        currentUserSabun: String?,
        currentUserCostCenterCode: String?,
        accountIds: List<Long>,
        teamMemberIds: List<Long>,
        year: Int,
        month: Int,
    ): List<FemaleEmployeeScheduleEventDto> {
        val costCenterCodes = resolveFetchAllCostCenterCodes(currentUserSabun, currentUserCostCenterCode)
        if (costCenterCodes.isEmpty()) return emptyList()

        val q = teamMemberSchedule
        val ymPredicate = q.workingDate.year().eq(year).and(q.workingDate.month().eq(month))
        val ccPredicate = q.employee.costCenterCode.`in`(costCenterCodes)

        val rows: List<TeamMemberSchedule> = when {
            teamMemberIds.isNotEmpty() -> queryFactory
                .selectFrom(q)
                .where(q.employee.id.`in`(teamMemberIds).and(ymPredicate).and(ccPredicate))
                .orderBy(q.employee.name.asc())
                .fetch()

            accountIds.isNotEmpty() -> queryFactory
                .selectFrom(q)
                .where(q.account.id.`in`(accountIds.map { it.toInt() }).and(ymPredicate).and(ccPredicate))
                .orderBy(q.account.name.asc())
                .fetch()

            else -> return emptyList()
        }

        return rows.map { it.toEventDto() }
    }

    /**
     * 월별 근무현황 요약 조회 (SF `fetchScheduleSummary`).
     *
     * BranchMapping 미적용 (D1=b SF 비대칭 유지) — 본인 cost_center_code 단일 일치.
     * 단, 하드코딩 사번이면 [SUMMARY_HARDCODED_CODES] 사용.
     */
    fun summary(
        currentUserSabun: String?,
        currentUserCostCenterCode: String?,
        year: Int,
        month: Int,
    ): List<FemaleEmployeeScheduleSummaryDto> {
        if (currentUserSabun.isNullOrBlank()) return emptyList()

        val femaleEmployees: List<Employee> = if (currentUserSabun in HARDCODED_LEADER_SABUNS) {
            employeeRepository.findByCostCenterCodeInAndRole(
                costCenterCodes = SUMMARY_HARDCODED_CODES.toList(),
                role = AppAuthority.WOMAN,
            )
        } else {
            if (currentUserCostCenterCode.isNullOrBlank()) return emptyList()
            employeeRepository.findByCostCenterCodeAndRole(
                costCenterCode = currentUserCostCenterCode,
                role = AppAuthority.WOMAN,
            )
        }

        if (femaleEmployees.isEmpty()) return emptyList()

        val empIds = femaleEmployees.mapNotNull { it.id.takeIf { v -> v != 0L } }
        if (empIds.isEmpty()) return emptyList()

        val q = teamMemberSchedule
        val rows = queryFactory.selectFrom(q)
            .where(
                q.employee.id.`in`(empIds)
                    .and(q.workingDate.year().eq(year))
                    .and(q.workingDate.month().eq(month))
            )
            .fetch()

        return aggregateSummary(rows, year, month)
    }

    /**
     * SF `fetchScheduleSummary` 의 AggregateResult 6벌 SOQL 을 in-memory 집계로 환원.
     * (분기 로직만 단위 테스트로 검증 가능하도록 분리)
     */
    internal fun aggregateSummary(
        rows: Collection<TeamMemberSchedule>,
        year: Int,
        month: Int,
    ): List<FemaleEmployeeScheduleSummaryDto> {
        val expectedMap = mutableMapOf<Int, Int>()
        val actualMap = mutableMapOf<Int, Int>()
        val expectedPromoMap = mutableMapOf<Int, Int>()
        val actualPromoMap = mutableMapOf<Int, Int>()
        val holidayMap = mutableMapOf<Int, Int>()
        val subHolidayMap = mutableMapOf<Int, Int>()

        for (row in rows) {
            val day = row.workingDate?.dayOfMonth ?: continue
            val wType = row.workingType
            val wCat1 = row.workingCategory1
            // 출근 여부 — attendance_log FK (Spec #749 R-2) 채워짐 여부로 판정. sfid 비즈니스 로직 사용 금지 정책 정합.
            val hasCommute = row.attendanceLog != null

            when {
                wType == WorkingType.WORK && wCat1 != WorkingCategory1.EVENT -> {
                    expectedMap.merge(day, 1) { a, b -> a + b }
                    if (hasCommute) actualMap.merge(day, 1) { a, b -> a + b }
                }
                wType == WorkingType.WORK && wCat1 == WorkingCategory1.EVENT -> {
                    expectedPromoMap.merge(day, 1) { a, b -> a + b }
                    if (hasCommute) actualPromoMap.merge(day, 1) { a, b -> a + b }
                }
                wType == WorkingType.ANNUAL_LEAVE -> holidayMap.merge(day, 1) { a, b -> a + b }
                wType == WorkingType.ALT_HOLIDAY -> subHolidayMap.merge(day, 1) { a, b -> a + b }
                else -> Unit
            }
        }

        return (1..31)
            .filter { d ->
                expectedMap[d] != null || actualMap[d] != null ||
                    expectedPromoMap[d] != null || actualPromoMap[d] != null ||
                    holidayMap[d] != null || subHolidayMap[d] != null
            }
            .map { d ->
                FemaleEmployeeScheduleSummaryDto(
                    year = year,
                    month = month,
                    day = d,
                    expected = expectedMap[d],
                    actual = actualMap[d],
                    expectedPromo = expectedPromoMap[d],
                    actualPromo = actualPromoMap[d],
                    holiday = holidayMap[d],
                    subHoliday = subHolidayMap[d],
                )
            }
    }

    /**
     * SF `fetchAllShcedule` 의 하드코딩 / BranchMapping 분기 결정 (분기 로직만 단위 테스트 가능하도록 분리).
     */
    internal fun resolveFetchAllCostCenterCodes(
        currentUserSabun: String?,
        currentUserCostCenterCode: String?,
    ): Set<String> = when {
        currentUserSabun in HARDCODED_LEADER_SABUNS -> FETCH_ALL_HARDCODED_CODES
        !currentUserCostCenterCode.isNullOrBlank() -> expander.expand(listOf(currentUserCostCenterCode))
        else -> emptySet()
    }

    /**
     * SF `EventObject.title` 생성 규칙 1:1 재현 (FullCalendarComponentController.cls:86-96).
     *
     * `EmpName(EmpCode) | ` 접두 + workingType='근무' 일 때 cat1/cat2/cat3/workingType/Account(ExternalKey,Type,BranchName)
     * + cat1='행사' 시 PromotionName 추가. 그 외 workingType.
     */
    private fun TeamMemberSchedule.toEventDto(): FemaleEmployeeScheduleEventDto {
        val emp = employee
        val acc = account
        val empName = emp?.name.orEmpty()
        val empCode = emp?.employeeCode.orEmpty()
        val workingTypeName = workingType?.displayName.orEmpty()

        val title = buildString {
            append("$empName($empCode) | ")
            if (workingType == WorkingType.WORK) {
                append(workingCategory1?.displayName.orEmpty()).append(" | ")
                append(workingCategory2?.displayName.orEmpty()).append(" | ")
                append(workingCategory3?.displayName.orEmpty()).append(" | ")
                append(workingTypeName).append(" | ")
                append(acc?.name.orEmpty())
                append("(")
                append(acc?.externalKey.orEmpty()).append(",")
                append(acc?.accountType?.displayName.orEmpty()).append(",")
                append(acc?.branchName.orEmpty())
                append(")")
                if (workingCategory1 == WorkingCategory1.EVENT) {
                    // SF `DKRetail__PromotionName__c` 동등 — backend 의 Promotion.Name (= promotionNumber).
                    val promoName = promotionEmployee?.promotion?.promotionNumber.orEmpty()
                    append(" | ").append(promoName)
                }
            } else {
                append(workingTypeName)
            }
        }

        return FemaleEmployeeScheduleEventDto(
            recordId = id,
            title = title,
            start = workingDate?.toString().orEmpty(),
            isClockIn = attendanceLog != null,
            accountId = acc?.id?.toLong(),
            employeeId = emp?.id,
            workingType = workingType?.displayName,
            workingCategory1 = workingCategory1?.displayName,
            workingCategory2 = workingCategory2?.displayName,
            workingCategory3 = workingCategory3?.displayName,
        )
    }

    companion object {
        /** SF FullCalendarComponentController 의 하드코딩 분기 대상 사번 (원미희 조장 외 3명). */
        internal val HARDCODED_LEADER_SABUNS: Set<String> =
            setOf("19951029", "20001013", "20060052", "20050308")

        /** SF fetchAllShcedule (L65, 76) 하드코딩 — 5개. */
        internal val FETCH_ALL_HARDCODED_CODES: Set<String> =
            setOf("3233", "3234", "3235", "3236", "5691")

        /** SF fetchScheduleSummary (L127) 하드코딩 — 6개 ('569' 추가). */
        internal val SUMMARY_HARDCODED_CODES: Set<String> =
            setOf("3234", "3233", "3235", "3236", "5691", "569")
    }
}
