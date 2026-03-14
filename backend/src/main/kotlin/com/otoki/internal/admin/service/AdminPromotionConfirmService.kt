package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.PromotionConfirmResponse
import com.otoki.internal.promotion.entity.Promotion
import com.otoki.internal.promotion.entity.PromotionEmployee
import com.otoki.internal.promotion.exception.*
import com.otoki.internal.promotion.repository.PromotionEmployeeRepository
import com.otoki.internal.promotion.repository.PromotionRepository
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AdminPromotionConfirmService(
    private val promotionRepository: PromotionRepository,
    private val promotionEmployeeRepository: PromotionEmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val userRepository: UserRepository
) {

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

        // 4. 검증에 필요한 데이터 사전 조회
        val employeeSfids = employees.mapNotNull { it.employeeSfid }.distinct()
        val scheduleDates = employees.mapNotNull { it.scheduleDate }.distinct()
        val peIdStrings = employees.map { it.id.toString() }

        // 기존 스케줄 조회 (검증 + Upsert용)
        val existingTeamMemberSchedulesByExt = teamMemberScheduleRepository.findByPromotionEmpIdExtIn(peIdStrings)
            .associateBy { it.promotionEmpIdExt }
        val existingTeamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdInAndWorkingDateIn(employeeSfids, scheduleDates)

        // 사원 정보 조회 (이름 + 상태 검증용)
        val userMap = userRepository.findBySfidIn(employeeSfids).associateBy { it.sfid }

        // 5. 검증 단계 (순서대로)
        validateRequiredValues(employees, userMap)
        validateDateRange(employees, promotion, userMap)
        validateWorkType3Limit(employees, existingTeamMemberSchedules, peIdStrings, userMap)
        validateLeaveConflict(employees, existingTeamMemberSchedules, peIdStrings, userMap)
        validateDuplicateSchedule(employees, existingTeamMemberSchedules, promotion, peIdStrings, userMap)
        validateEmployeeStatus(employees, userMap)

        // 6. Upsert 수행
        val accountIdStr = promotion.accountId.toString()
        val teamMemberSchedulesToSave = mutableListOf<TeamMemberSchedule>()

        for (pe in employees) {
            val peIdStr = pe.id.toString()
            val existing = existingTeamMemberSchedulesByExt[peIdStr]

            if (existing != null) {
                existing.updateForPromotion(
                    employeeId = pe.employeeSfid!!,
                    accountId = accountIdStr,
                    workingDate = pe.scheduleDate!!,
                    workingType = pe.workStatus!!,
                    workingCategory1 = pe.workType1!!,
                    workingCategory3 = pe.workType3!!,
                    workingCategory4 = pe.workType4,
                    promotionEmpId = peIdStr
                )
                teamMemberSchedulesToSave.add(existing)
            } else {
                val newTeamMemberSchedule = TeamMemberSchedule(
                    employeeId = pe.employeeSfid!!,
                    accountId = accountIdStr,
                    workingDate = pe.scheduleDate!!,
                    workingType = pe.workStatus!!,
                    workingCategory1 = pe.workType1!!,
                    workingCategory3 = pe.workType3!!,
                    workingCategory4 = pe.workType4,
                    promotionEmpId = peIdStr,
                    promotionEmpIdExt = peIdStr
                )
                teamMemberSchedulesToSave.add(newTeamMemberSchedule)
            }
        }

        val savedTeamMemberSchedules = teamMemberScheduleRepository.saveAll(teamMemberSchedulesToSave)

        // 7. PE에 schedule_id 역참조 저장
        val teamMemberScheduleByExt = savedTeamMemberSchedules.associateBy { it.promotionEmpIdExt }
        for (pe in employees) {
            val teamMemberSchedule = teamMemberScheduleByExt[pe.id.toString()]
            if (teamMemberSchedule != null) {
                pe.scheduleId = teamMemberSchedule.id
            }
        }
        promotionEmployeeRepository.saveAll(employees)

        return PromotionConfirmResponse(
            promotionId = promotionId,
            totalEmployees = employees.size,
            upsertedTeamMemberSchedules = savedTeamMemberSchedules.size
        )
    }

    private fun resolveEmployeeName(sfid: String, userMap: Map<String?, com.otoki.internal.sap.entity.User>): String {
        return userMap[sfid]?.name ?: sfid
    }

    // 검증 1: 필수값
    private fun validateRequiredValues(
        employees: List<PromotionEmployee>,
        userMap: Map<String?, com.otoki.internal.sap.entity.User>
    ) {
        for (pe in employees) {
            val missingFields = mutableListOf<String>()
            if (pe.employeeSfid.isNullOrBlank()) missingFields.add("employee_sfid")
            if (pe.scheduleDate == null) missingFields.add("schedule_date")
            if (pe.workStatus.isNullOrBlank()) missingFields.add("work_status")
            if (pe.workType1.isNullOrBlank()) missingFields.add("work_type1")
            if (pe.workType3.isNullOrBlank()) missingFields.add("work_type3")

            if (missingFields.isNotEmpty()) {
                val name = resolveEmployeeName(pe.employeeSfid ?: pe.id.toString(), userMap)
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
        userMap: Map<String?, com.otoki.internal.sap.entity.User>
    ) {
        for (pe in employees) {
            val scheduleDate = pe.scheduleDate!!
            if (scheduleDate < promotion.startDate || scheduleDate > promotion.endDate) {
                val name = resolveEmployeeName(pe.employeeSfid!!, userMap)
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
        currentPeIds: List<String>,
        userMap: Map<String?, com.otoki.internal.sap.entity.User>
    ) {
        // 기존 스케줄에서 현재 PE에 의해 생성된 스케줄 제외
        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmpIdExt == null || it.promotionEmpIdExt !in currentPeIds }

        // 기존 스케줄: 사원+날짜별 근무유형3 카운트
        data class EmpDateKey(val employeeId: String, val date: LocalDate)

        val existingCounts = mutableMapOf<EmpDateKey, MutableMap<String, Int>>()
        for (teamMemberSchedule in externalTeamMemberSchedules) {
            val key = EmpDateKey(teamMemberSchedule.employeeId ?: continue, teamMemberSchedule.workingDate ?: continue)
            val type3 = teamMemberSchedule.workingCategory3 ?: continue
            existingCounts.getOrPut(key) { mutableMapOf() }
                .merge(type3, 1) { a, b -> a + b }
        }

        // 신규 PE: 사원+날짜별 근무유형3 카운트
        val newCounts = mutableMapOf<EmpDateKey, MutableMap<String, Int>>()
        for (pe in employees) {
            val key = EmpDateKey(pe.employeeSfid!!, pe.scheduleDate!!)
            newCounts.getOrPut(key) { mutableMapOf() }
                .merge(pe.workType3!!, 1) { a, b -> a + b }
        }

        // 모든 사원+날짜 조합 검증
        val allKeys = (existingCounts.keys + newCounts.keys).distinct()
        for (key in allKeys) {
            val existing = existingCounts[key] ?: emptyMap()
            val incoming = newCounts[key] ?: continue

            val totalFixed = (existing["고정"] ?: 0) + (incoming["고정"] ?: 0)
            val totalAlternate = (existing["격고"] ?: 0) + (incoming["격고"] ?: 0)
            val totalTraversal = (existing["순회"] ?: 0) + (incoming["순회"] ?: 0)

            val name = resolveEmployeeName(key.employeeId, userMap)

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
        currentPeIds: List<String>,
        userMap: Map<String?, com.otoki.internal.sap.entity.User>
    ) {
        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmpIdExt == null || it.promotionEmpIdExt !in currentPeIds }

        data class EmpDateKey(val employeeId: String, val date: LocalDate)

        val existingByKey = externalTeamMemberSchedules.groupBy {
            EmpDateKey(it.employeeId ?: "", it.workingDate ?: LocalDate.MIN)
        }

        for (pe in employees) {
            val key = EmpDateKey(pe.employeeSfid!!, pe.scheduleDate!!)
            val existing = existingByKey[key] ?: continue
            val name = resolveEmployeeName(pe.employeeSfid!!, userMap)

            // 기존에 연차/대휴가 있으면 충돌
            val hasLeave = existing.any { it.workingType == "연차" || it.workingType == "대휴" }
            if (hasLeave) {
                throw LeaveConflictException("${name}의 ${pe.scheduleDate}에 예정된 연차/대휴가 존재합니다")
            }

            // PE가 연차/대휴인데 기존에 근무 스케줄이 있으면 충돌
            if (pe.workStatus == "연차" || pe.workStatus == "대휴") {
                val hasWork = existing.any { it.workingType != "연차" && it.workingType != "대휴" }
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
        currentPeIds: List<String>,
        userMap: Map<String?, com.otoki.internal.sap.entity.User>
    ) {
        val accountIdStr = promotion.accountId.toString()
        val externalTeamMemberSchedules = existingTeamMemberSchedules.filter { it.promotionEmpIdExt == null || it.promotionEmpIdExt !in currentPeIds }

        for (pe in employees) {
            val duplicate = externalTeamMemberSchedules.any {
                it.employeeId == pe.employeeSfid!! &&
                    it.workingDate == pe.scheduleDate!! &&
                    it.accountId == accountIdStr
            }
            if (duplicate) {
                val name = resolveEmployeeName(pe.employeeSfid!!, userMap)
                throw DuplicateScheduleException("${name}의 ${pe.scheduleDate}에 동일 거래처 근무 일정이 존재합니다")
            }
        }
    }

    // 검증 6: 여사원 상태
    private fun validateEmployeeStatus(
        employees: List<PromotionEmployee>,
        userMap: Map<String?, com.otoki.internal.sap.entity.User>
    ) {
        for (pe in employees) {
            val user = userMap[pe.employeeSfid] ?: continue
            val name = user.name

            when (user.status) {
                "휴직" -> throw EmployeeOnLeaveException("${name}은 휴직 상태입니다")
                "퇴직" -> throw EmployeeResignedException("${name}은 퇴직하였습니다")
            }
        }
    }
}
