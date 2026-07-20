package com.otoki.powersales.domain.activity.promotion.service

import com.otoki.powersales.domain.activity.promotion.dto.response.PromotionConfirmResponse
import com.otoki.powersales.domain.activity.promotion.entity.Promotion
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.activity.promotion.exception.DateOutOfRangeException
import com.otoki.powersales.domain.activity.promotion.exception.DuplicateScheduleException
import com.otoki.powersales.domain.activity.promotion.exception.EmployeeOnLeaveException
import com.otoki.powersales.domain.activity.promotion.exception.EmployeeResignedException
import com.otoki.powersales.domain.activity.promotion.exception.LeaveConflictException
import com.otoki.powersales.domain.activity.promotion.exception.NoEmployeesException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionAccountRequiredException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionDateRequiredException
import com.otoki.powersales.domain.activity.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.domain.activity.promotion.exception.ValuesRequiredException
import com.otoki.powersales.domain.activity.promotion.exception.WorkType3LimitExceededException
import com.otoki.powersales.domain.activity.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.domain.activity.promotion.repository.PromotionRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleOwnerResolver
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleNameGenerator
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.collections.contains

@Component
class PromotionSchedulesUpsertHelper(
    private val promotionRepository: PromotionRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val teamMemberScheduleNameGenerator: TeamMemberScheduleNameGenerator,
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver
) {

    companion object {
        private val DEFAULT_WORK_TYPE1 = WorkingCategory1.EVENT
        private val DEFAULT_WORK_STATUS = WorkingType.WORK
    }

    @Transactional
    fun upsert(promotionId: Long): PromotionConfirmResponse {
        val promotion = promotionRepository.findById(promotionId)
            .filter { !it.isDeleted }
            .orElseThrow { PromotionNotFoundException() }

        // 행사 시작일/종료일 필수 — 레거시 PromotionToScheduleQuickActionController:8-11 ('DateRequired') 동등.
        // 투입일 범위 검증(validateDateRange)이 두 값을 전제로 하므로 그보다 먼저 막는다.
        if (promotion.startDate == null || promotion.endDate == null) {
            throw PromotionDateRequiredException()
        }

        val employees = promotionEmployeeRepository.findByPromotionId(promotionId)
        if (employees.isEmpty()) {
            throw NoEmployeesException()
        }

        var defaultsCorrected = false
        for (pe in employees) {
            if (pe.workType1 == null) { pe.workType1 = DEFAULT_WORK_TYPE1; defaultsCorrected = true }
            if (pe.workStatus == null) { pe.workStatus = DEFAULT_WORK_STATUS; defaultsCorrected = true }
        }
        if (defaultsCorrected) {
            promotionEmployeeRepository.saveAll(employees)
        }

        val employeeIds = employees.mapNotNull { it.employeeId }.distinct()
        val scheduleDates = employees.mapNotNull { it.scheduleDate }.distinct()
        val peIds = employees.map { it.id }

        // 동일 인자 findAllById 를 두 번 호출하지 않도록 1회 조회 결과를 map/list 양쪽에 재사용한다.
        val employeeEntities: List<Employee> = if (employeeIds.isNotEmpty()) {
            employeeRepository.findAllById(employeeIds).toList()
        } else emptyList()
        val userByIdMap: Map<Long, Employee> = employeeEntities.associateBy { it.id }

        val existingTeamMemberSchedulesByPeId = teamMemberScheduleRepository.findByPromotionEmployeeIn(employees)
            .associateBy { it.promotionEmployee?.id }
        val existingTeamMemberSchedules = if (employeeEntities.isNotEmpty()) {
            teamMemberScheduleRepository.findByEmployeeInAndWorkingDateIn(employeeEntities, scheduleDates)
        } else emptyList()

        validateRequiredValues(employees, userByIdMap)
        validateAccountRequired(employees, promotion)
        validateDateRange(employees, promotion, userByIdMap)
        validateWorkType3Limit(employees, existingTeamMemberSchedules, peIds, userByIdMap)
        validateLeaveConflict(employees, existingTeamMemberSchedules, peIds, userByIdMap)
        validateDuplicateSchedule(employees, existingTeamMemberSchedules, promotion, peIds, userByIdMap)
        validateEmployeeStatus(employees, userByIdMap)

        // owner = 대상 직원의 소속 조장 User (레거시 TeamMemberScheduleTriggerHandler.insertOwner 동등).
        // 레거시는 insert(beforeInsert) 시점에만 owner 를 지정하므로 신규 row 에만 적용한다.
        val ownerByCostCenterCode = teamMemberScheduleOwnerResolver
            .resolveOwnersByCostCenterCode(userByIdMap.values)

        // name 채번은 신규 생성분 개수만큼 한 번에 발급받는다. 건별 채번은 쿼리마다 team_member_schedule
        // 전체를 스캔(MAX(regexp_replace(name, ...)))하므로 확정 건수에 비례해 느려진다.
        val newScheduleCount = employees.count { existingTeamMemberSchedulesByPeId[it.id] == null }
        val nameIterator = teamMemberScheduleNameGenerator.nextBatch(newScheduleCount).iterator()

        val teamMemberSchedulesToSave = mutableListOf<TeamMemberSchedule>()
        for (pe in employees) {
            val existing = existingTeamMemberSchedulesByPeId[pe.id]
            val empEntity = userByIdMap[pe.employeeId!!]!!

            if (existing != null) {
                existing.updateForPromotion(
                    employee = empEntity,
                    account = promotion.account,
                    workingDate = pe.scheduleDate!!,
                    workingType = pe.workStatus!!,
                    workingCategory1 = pe.workType1!!,
                    workingCategory3 = pe.workType3!!,
                    workingCategory4 = null,
                    promotionEmployee = pe
                )
                teamMemberSchedulesToSave.add(existing)
            } else {
                val newTeamMemberSchedule = TeamMemberSchedule(
                    name = nameIterator.next(),
                    employee = empEntity,
                    account = promotion.account,
                    workingDate = pe.scheduleDate!!,
                    workingType = pe.workStatus!!,
                    workingCategory1 = pe.workType1!!,
                    workingCategory3 = pe.workType3!!,
                    workingCategory4 = null,
                    promotionEmployee = pe,
                    ownerUser = empEntity.costCenterCode?.let { ownerByCostCenterCode[it] }
                )
                teamMemberSchedulesToSave.add(newTeamMemberSchedule)
            }
        }

        val savedTeamMemberSchedules = teamMemberScheduleRepository.saveAll(teamMemberSchedulesToSave)

        val teamMemberScheduleByPeId = savedTeamMemberSchedules.associateBy { it.promotionEmployee?.id }
        for (pe in employees) {
            val teamMemberSchedule = teamMemberScheduleByPeId[pe.id]
            if (teamMemberSchedule != null) {
                pe.teamMemberScheduleId = teamMemberSchedule.id
            }
        }
        promotionEmployeeRepository.saveAll(employees)

        return PromotionConfirmResponse(
            promotionId = promotionId,
            totalEmployees = employees.size,
            upsertedTeamMemberSchedules = savedTeamMemberSchedules.size
        )
    }

    private fun resolveEmployeeName(employeeId: Long, userByIdMap: Map<Long, Employee>): String {
        return userByIdMap[employeeId]?.name ?: employeeId.toString()
    }

    private fun validateRequiredValues(
        employees: List<PromotionEmployee>,
        userByIdMap: Map<Long, Employee>
    ) {
        for (pe in employees) {
            val missingFields = mutableListOf<String>()
            if (pe.employeeId == null) missingFields.add("행사사원")
            if (pe.scheduleDate == null) missingFields.add("투입일")
            if (pe.workStatus == null) missingFields.add("근무상태")
            if (pe.workType1 == null) missingFields.add("근무유형1")
            if (pe.workType3 == null) missingFields.add("근무유형3")
            // 기준단가·목표수량·목표금액은 확정 필수에서 제외.
            // SF 레거시(PromotionToScheduleQuickActionController:34-36) 확정 필수는 직원·투입일·근무유형1·
            // 근무유형3·근무상태 5개뿐이다. 기준단가/목표수량은 SF 확정 검증 대상이 아니었고, 목표금액은
            // formula(required=false, BlankAsZero)라 애초에 검증할 수 없는 값이었다. SF 마이그레이션 적재 row 는
            // 이 값들이 미매핑 NULL 이라 확정 필수로 두면 이관분이 전건 확정 탈락 → 레거시 동등 복원.

            if (missingFields.isNotEmpty()) {
                val name = pe.employeeId?.let { resolveEmployeeName(it, userByIdMap) } ?: pe.id.toString()
                throw ValuesRequiredException(
                    "${name}의 필수 항목을 입력하세요 (${missingFields.joinToString(", ")})"
                )
            }
        }
    }

    /**
     * 근무 상태 행사조원이 있으면 행사마스터의 거래처가 필수 — 레거시
     * `TeamMemberScheduleTriggerHandler.IsAccIdEmpty()` (`근무 && AccountId__c == null` → addError) 동등.
     *
     * 확정 시 여사원일정의 거래처는 행사마스터의 거래처를 그대로 옮겨 담으므로, 행사마스터에 거래처가
     * 비어 있으면 근무 일정을 만들 수 없다. 레거시와 동일하게 연차/대휴만 있는 행사는 통과시킨다.
     */
    private fun validateAccountRequired(
        employees: List<PromotionEmployee>,
        promotion: Promotion
    ) {
        if (promotion.account != null) return
        if (employees.any { it.workStatus == WorkingType.WORK }) {
            throw PromotionAccountRequiredException()
        }
    }

    private fun validateDateRange(
        employees: List<PromotionEmployee>,
        promotion: Promotion,
        userByIdMap: Map<Long, Employee>
    ) {
        for (pe in employees) {
            val scheduleDate = pe.scheduleDate!!
            if (scheduleDate < promotion.startDate || scheduleDate > promotion.endDate) {
                val name = resolveEmployeeName(pe.employeeId!!, userByIdMap)
                throw DateOutOfRangeException(
                    "${name}의 투입일이 행사 기간(${promotion.startDate} ~ ${promotion.endDate})을 벗어납니다"
                )
            }
        }
    }

    private fun validateWorkType3Limit(
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule>,
        currentPeIds: List<Long>,
        userByIdMap: Map<Long, Employee>
    ) {
        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmployee?.id == null || it.promotionEmployee?.id !in currentPeIds }

        data class EmpDateKey(val employeeId: Long, val date: LocalDate)

        val existingCounts = mutableMapOf<EmpDateKey, MutableMap<String, Int>>()
        for (teamMemberSchedule in externalTeamMemberSchedules) {
            val key = EmpDateKey(teamMemberSchedule.employee?.id ?: continue, teamMemberSchedule.workingDate ?: continue)
            val type3 = teamMemberSchedule.workingCategory3?.displayName ?: continue
            existingCounts.getOrPut(key) { mutableMapOf() }
                .merge(type3, 1) { a, b -> a + b }
        }

        val newCounts = mutableMapOf<EmpDateKey, MutableMap<String, Int>>()
        for (pe in employees) {
            val key = EmpDateKey(pe.employeeId!!, pe.scheduleDate!!)
            newCounts.getOrPut(key) { mutableMapOf() }
                .merge(pe.workType3!!.displayName, 1) { a, b -> a + b }
        }

        val allKeys = (existingCounts.keys + newCounts.keys).distinct()
        for (key in allKeys) {
            val existing = existingCounts[key] ?: emptyMap()
            val incoming = newCounts[key] ?: continue

            val totalFixed = (existing["고정"] ?: 0) + (incoming["고정"] ?: 0)
            val totalAlternate = (existing["격고"] ?: 0) + (incoming["격고"] ?: 0)
            val totalTraversal = (existing["순회"] ?: 0) + (incoming["순회"] ?: 0)

            val name = resolveEmployeeName(key.employeeId, userByIdMap)

            if (totalFixed > 0) {
                if (totalFixed > 1) {
                    throw WorkType3LimitExceededException("${name}의 ${key.date}에 고정 일정이 최대 수량을 초과합니다")
                }
                if (totalAlternate > 0 || totalTraversal > 0) {
                    throw WorkType3LimitExceededException("${name}의 ${key.date}에 고정 일정이 최대 수량을 초과합니다")
                }
            }

            if (totalAlternate > 2) {
                throw WorkType3LimitExceededException("${name}의 ${key.date}에 격고 일정이 최대 수량을 초과합니다")
            }

            if (totalAlternate >= 2 && totalTraversal > 0) {
                throw WorkType3LimitExceededException("${name}의 ${key.date}에 격고 일정이 최대 수량을 초과합니다")
            }
        }
    }

    private fun validateLeaveConflict(
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule>,
        currentPeIds: List<Long>,
        userByIdMap: Map<Long, Employee>
    ) {
        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmployee?.id == null || it.promotionEmployee?.id !in currentPeIds }

        data class EmpDateKey(val employeeId: Long, val date: LocalDate)

        val existingByKey = externalTeamMemberSchedules.groupBy {
            EmpDateKey(it.employee?.id ?: 0L, it.workingDate ?: LocalDate.MIN)
        }

        for (pe in employees) {
            val key = EmpDateKey(pe.employeeId!!, pe.scheduleDate!!)
            val existing = existingByKey[key] ?: continue
            val name = resolveEmployeeName(pe.employeeId!!, userByIdMap)

            val hasLeave = existing.any { it.workingType == WorkingType.ANNUAL_LEAVE || it.workingType == WorkingType.ALT_HOLIDAY }
            if (hasLeave) {
                throw LeaveConflictException("${name}의 ${pe.scheduleDate}에 예정된 연차/대휴가 존재합니다")
            }

            if (pe.workStatus == WorkingType.ANNUAL_LEAVE || pe.workStatus == WorkingType.ALT_HOLIDAY) {
                val hasWork = existing.any { it.workingType != WorkingType.ANNUAL_LEAVE && it.workingType != WorkingType.ALT_HOLIDAY }
                if (hasWork) {
                    throw LeaveConflictException("${name}의 ${pe.scheduleDate}에 예정된 연차/대휴가 존재합니다")
                }
            }
        }
    }

    private fun validateDuplicateSchedule(
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule>,
        promotion: Promotion,
        currentPeIds: List<Long>,
        userByIdMap: Map<Long, Employee>
    ) {
        // 거래처 없는 행사(연차/대휴 전용)는 "동일 거래처 중복" 판정 자체가 성립하지 않으므로 건너뛴다.
        val promotionAccountId = promotion.account?.id ?: return

        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmployee?.id == null || it.promotionEmployee?.id !in currentPeIds }

        for (pe in employees) {
            val duplicate = externalTeamMemberSchedules.any {
                it.employee?.id == pe.employeeId &&
                    it.workingDate == pe.scheduleDate!! &&
                    it.account?.id == promotionAccountId
            }
            if (duplicate) {
                val name = resolveEmployeeName(pe.employeeId!!, userByIdMap)
                throw DuplicateScheduleException("${name}의 ${pe.scheduleDate}에 동일 거래처 근무 일정이 존재합니다")
            }
        }
    }

    private fun validateEmployeeStatus(
        employees: List<PromotionEmployee>,
        userByIdMap: Map<Long, Employee>
    ) {
        for (pe in employees) {
            val employee = pe.employeeId?.let { userByIdMap[it] } ?: continue
            val name = employee.name

            when (employee.status) {
                "휴직" -> throw EmployeeOnLeaveException("${name}은 휴직 상태입니다")
                "퇴직" -> throw EmployeeResignedException("${name}은 퇴직하였습니다")
            }
        }
    }
}
