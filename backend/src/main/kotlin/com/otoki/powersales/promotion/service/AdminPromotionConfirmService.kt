package com.otoki.powersales.promotion.service

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.promotion.dto.response.PromotionConfirmResponse
import com.otoki.powersales.promotion.entity.Promotion
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.promotion.exception.*
import com.otoki.powersales.promotion.repository.PromotionEmployeeRepository
import com.otoki.powersales.promotion.repository.PromotionRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminPromotionConfirmService(
    private val promotionRepository: PromotionRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val employeeRepository: EmployeeRepository
) {

    companion object {
        private val DEFAULT_WORK_TYPE1 = WorkingCategory1.EVENT
        private val DEFAULT_WORK_STATUS = WorkingType.WORK
    }

    @Transactional
    fun confirmPromotion(promotionId: Long): PromotionConfirmResponse {
        // 1. 행사마스터 조회
        val promotion = promotionRepository.findById(promotionId)
            .filter { !it.isDeleted }
            .orElseThrow { PromotionNotFoundException() }

        // 2~3. PE 목록 조회
        val employees = promotionEmployeeRepository.findByPromotionId(promotionId)
        if (employees.isEmpty()) {
            throw NoEmployeesException()
        }

        // 3.5. workType1/workStatus 기본값 보정
        var defaultsCorrected = false
        for (pe in employees) {
            if (pe.workType1 == null) { pe.workType1 = DEFAULT_WORK_TYPE1; defaultsCorrected = true }
            if (pe.workStatus == null) { pe.workStatus = DEFAULT_WORK_STATUS; defaultsCorrected = true }
        }
        if (defaultsCorrected) {
            promotionEmployeeRepository.saveAll(employees)
        }

        // 4. 검증에 필요한 데이터 사전 조회
        val employeeIds = employees.mapNotNull { it.employeeId }.distinct()
        val scheduleDates = employees.mapNotNull { it.scheduleDate }.distinct()
        val peIds = employees.map { it.id }

        // 사원 정보 조회 (이름 + 상태 검증용)
        val userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee> = if (employeeIds.isNotEmpty()) {
            employeeRepository.findAllById(employeeIds).associateBy { it.id }
        } else emptyMap()

        // 기존 스케줄 조회 (검증 + Upsert용)
        val existingTeamMemberSchedulesByPeId = teamMemberScheduleRepository.findByPromotionEmployeeIn(employees)
            .associateBy { it.promotionEmployee?.id }
        val employeeEntities = if (employeeIds.isNotEmpty()) {
            employeeRepository.findAllById(employeeIds)
        } else emptyList()
        val existingTeamMemberSchedules = if (employeeEntities.isNotEmpty()) {
            teamMemberScheduleRepository.findByEmployeeInAndWorkingDateIn(employeeEntities, scheduleDates)
        } else emptyList()

        // 5. 검증 단계 (순서대로)
        validateRequiredValues(employees, userByIdMap)
        validateDateRange(employees, promotion, userByIdMap)
        validateWorkType3Limit(employees, existingTeamMemberSchedules, peIds, userByIdMap)
        validateLeaveConflict(employees, existingTeamMemberSchedules, peIds, userByIdMap)
        validateDuplicateSchedule(employees, existingTeamMemberSchedules, promotion, peIds, userByIdMap)
        validateEmployeeStatus(employees, userByIdMap)

        // 6. Upsert 수행
        val teamMemberSchedulesToSave = mutableListOf<TeamMemberSchedule>()

        val employeeEntityMap = userByIdMap

        for (pe in employees) {
            val existing = existingTeamMemberSchedulesByPeId[pe.id]

            val empEntity = employeeEntityMap[pe.employeeId!!]!!

            if (existing != null) {
                existing.updateForPromotion(
                    employee = empEntity,
                    account = promotion.account!!,
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
                    employee = empEntity,
                    account = promotion.account,
                    workingDate = pe.scheduleDate!!,
                    workingType = pe.workStatus!!,
                    workingCategory1 = pe.workType1!!,
                    workingCategory3 = pe.workType3!!,
                    workingCategory4 = null,
                    promotionEmployee = pe
                )
                teamMemberSchedulesToSave.add(newTeamMemberSchedule)
            }
        }

        val savedTeamMemberSchedules = teamMemberScheduleRepository.saveAll(teamMemberSchedulesToSave)

        // 7. PE에 schedule_id 역참조 저장
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

    private fun resolveEmployeeName(employeeId: Long, userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee>): String {
        return userByIdMap[employeeId]?.name ?: employeeId.toString()
    }

    // 검증 1: 필수값
    private fun validateRequiredValues(
        employees: List<PromotionEmployee>,
        userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee>
    ) {
        for (pe in employees) {
            val missingFields = mutableListOf<String>()
            if (pe.employeeId == null) missingFields.add("행사사원")
            if (pe.scheduleDate == null) missingFields.add("투입일")
            if (pe.workStatus == null) missingFields.add("근무상태")
            if (pe.workType1 == null) missingFields.add("근무유형1")
            if (pe.workType3 == null) missingFields.add("근무유형3")

            if (missingFields.isNotEmpty()) {
                val name = pe.employeeId?.let { resolveEmployeeName(it, userByIdMap) } ?: pe.id.toString()
                throw ValuesRequiredException(
                    "${name}의 필수 항목을 입력하세요 (${missingFields.joinToString(", ")})"
                )
            }
        }
    }

    // 검증 2: 투입일 범위
    private fun validateDateRange(
        employees: List<PromotionEmployee>,
        promotion: Promotion,
        userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee>
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

    // 검증 3: 근무유형3 수량 제한
    private fun validateWorkType3Limit(
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule>,
        currentPeIds: List<Long>,
        userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee>
    ) {
        // 기존 스케줄에서 현재 PE에 의해 생성된 스케줄 제외
        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmployee?.id == null || it.promotionEmployee?.id !in currentPeIds }

        // 기존 스케줄: 사원+날짜별 근무유형3 카운트
        data class EmpDateKey(val employeeId: Long, val date: LocalDate)

        val existingCounts = mutableMapOf<EmpDateKey, MutableMap<String, Int>>()
        for (teamMemberSchedule in externalTeamMemberSchedules) {
            val key = EmpDateKey(teamMemberSchedule.employee?.id ?: continue, teamMemberSchedule.workingDate ?: continue)
            val type3 = teamMemberSchedule.workingCategory3?.displayName ?: continue
            existingCounts.getOrPut(key) { mutableMapOf() }
                .merge(type3, 1) { a, b -> a + b }
        }

        // 신규 PE: 사원+날짜별 근무유형3 카운트
        val newCounts = mutableMapOf<EmpDateKey, MutableMap<String, Int>>()
        for (pe in employees) {
            val key = EmpDateKey(pe.employeeId!!, pe.scheduleDate!!)
            newCounts.getOrPut(key) { mutableMapOf() }
                .merge(pe.workType3!!.displayName, 1) { a, b -> a + b }
        }

        // 모든 사원+날짜 조합 검증
        val allKeys = (existingCounts.keys + newCounts.keys).distinct()
        for (key in allKeys) {
            val existing = existingCounts[key] ?: emptyMap()
            val incoming = newCounts[key] ?: continue

            val totalFixed = (existing["고정"] ?: 0) + (incoming["고정"] ?: 0)
            val totalAlternate = (existing["격고"] ?: 0) + (incoming["격고"] ?: 0)
            val totalTraversal = (existing["순회"] ?: 0) + (incoming["순회"] ?: 0)

            val name = resolveEmployeeName(key.employeeId, userByIdMap)

            // 고정 검증
            if (totalFixed > 0) {
                if (totalFixed > 1) {
                    throw WorkType3LimitExceededException("${name}의 ${key.date}에 고정 일정이 최대 수량을 초과합니다")
                }
                if (totalAlternate > 0 || totalTraversal > 0) {
                    throw WorkType3LimitExceededException("${name}의 ${key.date}에 고정 일정이 최대 수량을 초과합니다")
                }
            }

            // 격고 검증
            if (totalAlternate > 2) {
                throw WorkType3LimitExceededException("${name}의 ${key.date}에 격고 일정이 최대 수량을 초과합니다")
            }

            // 격고 2개이면 순회 추가 불가
            if (totalAlternate >= 2 && totalTraversal > 0) {
                throw WorkType3LimitExceededException("${name}의 ${key.date}에 격고 일정이 최대 수량을 초과합니다")
            }
        }
    }

    // 검증 4: 연차/대휴 충돌
    private fun validateLeaveConflict(
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule>,
        currentPeIds: List<Long>,
        userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee>
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

            // 기존에 연차/대휴가 있으면 충돌
            val hasLeave = existing.any { it.workingType == WorkingType.ANNUAL_LEAVE || it.workingType == WorkingType.ALT_HOLIDAY }
            if (hasLeave) {
                throw LeaveConflictException("${name}의 ${pe.scheduleDate}에 예정된 연차/대휴가 존재합니다")
            }

            // PE가 연차/대휴인데 기존에 근무 스케줄이 있으면 충돌
            if (pe.workStatus == WorkingType.ANNUAL_LEAVE || pe.workStatus == WorkingType.ALT_HOLIDAY) {
                val hasWork = existing.any { it.workingType != WorkingType.ANNUAL_LEAVE && it.workingType != WorkingType.ALT_HOLIDAY }
                if (hasWork) {
                    throw LeaveConflictException("${name}의 ${pe.scheduleDate}에 예정된 연차/대휴가 존재합니다")
                }
            }
        }
    }

    // 검증 5: 거래처 중복
    private fun validateDuplicateSchedule(
        employees: List<PromotionEmployee>,
        existingTeamMemberSchedules: List<TeamMemberSchedule>,
        promotion: Promotion,
        currentPeIds: List<Long>,
        userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee>
    ) {
        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmployee?.id == null || it.promotionEmployee?.id !in currentPeIds }

        for (pe in employees) {
            val duplicate = externalTeamMemberSchedules.any {
                it.employee?.id == pe.employeeId &&
                    it.workingDate == pe.scheduleDate!! &&
                    it.account?.id == promotion.account!!.id
            }
            if (duplicate) {
                val name = resolveEmployeeName(pe.employeeId!!, userByIdMap)
                throw DuplicateScheduleException("${name}의 ${pe.scheduleDate}에 동일 거래처 근무 일정이 존재합니다")
            }
        }
    }

    // 검증 6: 여사원 상태
    private fun validateEmployeeStatus(
        employees: List<PromotionEmployee>,
        userByIdMap: Map<Long, com.otoki.powersales.employee.entity.Employee>
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
