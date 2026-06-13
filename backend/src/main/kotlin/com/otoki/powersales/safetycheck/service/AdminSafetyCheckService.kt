package com.otoki.powersales.safetycheck.service

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.safetycheck.dto.response.EquipmentStatus
import com.otoki.powersales.safetycheck.dto.response.MemberStatus
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckStatusResponse
import com.otoki.powersales.schedule.exception.TeamScheduleEmployeeNotFoundException
import com.otoki.powersales.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.safetycheck.repository.SafetyCheckItemRepository
import com.otoki.powersales.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class AdminSafetyCheckService(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository,
    private val safetyCheckItemRepository: SafetyCheckItemRepository,
    private val accountRepository: AccountRepository
) {

    private var equipmentLabels: Map<Int, String> = emptyMap()

    @PostConstruct
    fun initEquipmentLabels() {
        equipmentLabels = safetyCheckItemRepository
            .findByUseYnOrderByQuestionNumAscSeqNumAsc("Y")
            .filter { it.questionNum == 1 }
            .associate { it.seqNum to it.contents }
    }

    fun getStatus(userId: Long, date: LocalDate): SafetyCheckStatusResponse {
        val currentEmployee = findEmployeeById(userId)
        val costCenterCode = currentEmployee.costCenterCode
            ?: return emptyResponse(date)

        // 1. 소속 여사원 조회 — 본부 통합 권한 (costCenterCode 3232) 은 3234·3236 두 사업소 통합
        val members = if (costCenterCode == SF_HQ_INTEGRATED_COST_CENTER) {
            employeeRepository.findByCostCenterCodeInAndRole(SF_HQ_INTEGRATED_TARGETS, AppAuthority.WOMAN)
        } else {
            employeeRepository.findByCostCenterCodeAndRole(costCenterCode, AppAuthority.WOMAN)
        }.filter { it.isDeleted != true }
        if (members.isEmpty()) return emptyResponse(date)

        val employeeIds = members.map { it.id }

        // 2. 스케줄 벌크 조회 + 근무 필터 + 사원당 1건 선택
        val schedules = teamMemberScheduleRepository
            .findByWorkingDateAndEmployeeIn(date, members)
            .filter { it.workingType == WorkingType.WORK && it.isDeleted != true }
        val scheduleByEmployee = selectOneSchedulePerEmployee(schedules)

        if (scheduleByEmployee.isEmpty()) return emptyResponse(date)

        // 3. 안전점검 제출 벌크 조회
        val scheduledEmployeeIds = scheduleByEmployee.keys.toList()
        val submissions = safetyCheckSubmissionRepository
            .findByEmployeeIdInAndWorkingDate(scheduledEmployeeIds, date)
        val submissionByEmployee = submissions.associateBy { it.employeeId }

        // 4. 거래처 벌크 조회
        val accountIds = scheduleByEmployee.values.mapNotNull { it.account?.id }.distinct()
        val accountMap = if (accountIds.isNotEmpty()) {
            accountRepository.findByIdIn(accountIds).associateBy { it.id }
        } else emptyMap()

        // 5. 사원 Map
        val employeeMap = members.associateBy { it.id }

        // 6. 응답 생성
        val memberStatuses = scheduledEmployeeIds.mapNotNull { empId ->
            val employee = employeeMap[empId] ?: return@mapNotNull null
            val schedule = scheduleByEmployee[empId] ?: return@mapNotNull null
            val submission = submissionByEmployee[empId]
            val account = schedule.account?.id?.let { accountMap[it] }

            buildMemberStatus(employee, submission, account, schedule)
        }.sortedBy { it.employeeName }

        val submittedCount = memberStatuses.count { it.submitted }
        return SafetyCheckStatusResponse(
            date = date.toString(),
            totalCount = memberStatuses.size,
            submittedCount = submittedCount,
            notSubmittedCount = memberStatuses.size - submittedCount,
            members = memberStatuses
        )
    }

    private fun selectOneSchedulePerEmployee(schedules: List<TeamMemberSchedule>): Map<Long, TeamMemberSchedule> {
        return schedules
            .groupBy { it.employee?.id ?: 0L }
            .filterKeys { it != 0L }
            .mapValues { (_, list) ->
                list.firstOrNull { it.traversalFlag == "O" }
                    ?: list.firstOrNull { it.traversalFlag == null }
                    ?: list.first()
            }
    }

    private fun buildMemberStatus(
        employee: Employee,
        submission: SafetyCheckSubmission?,
        account: Account?,
        schedule: TeamMemberSchedule
    ): MemberStatus {
        val submitted = submission != null
        val equipments = if (submitted) buildEquipmentList(submission!!) else emptyList()

        return MemberStatus(
            id = employee.id,
            employeeCode = employee.employeeCode,
            employeeName = employee.name,
            accountCode = account?.externalKey,
            accountName = account?.name,
            submitted = submitted,
            submittedAt = submission?.completeTime,
            startTime = submission?.startTime,
            equipments = equipments,
            yesCount = submission?.yesCheckCount ?: 0,
            noCount = submission?.noCheckCount ?: 0,
            precautions = submission?.precaution,
            precautionCount = submission?.precautionCheckCount ?: 0,
            workReportStatus = schedule.isWorkReport
        )
    }

    private fun buildEquipmentList(submission: SafetyCheckSubmission): List<EquipmentStatus> {
        val equipmentValues = listOf(
            submission.equipment1, submission.equipment2, submission.equipment3,
            submission.equipment4, submission.equipment5, submission.equipment6,
            submission.equipment7, submission.equipment8, submission.equipment9
        )
        return equipmentValues.mapIndexedNotNull { index, answer ->
            if (answer != null) {
                EquipmentStatus(
                    seqNum = index + 1,
                    label = equipmentLabels[index + 1] ?: "항목 ${index + 1}",
                    answer = answer
                )
            } else null
        }
    }

    private fun emptyResponse(date: LocalDate): SafetyCheckStatusResponse {
        return SafetyCheckStatusResponse(
            date = date.toString(),
            totalCount = 0,
            submittedCount = 0,
            notSubmittedCount = 0,
            members = emptyList()
        )
    }

    private fun findEmployeeById(userId: Long): Employee {
        return employeeRepository.findById(userId)
            .orElseThrow { TeamScheduleEmployeeNotFoundException() }
    }

    companion object {
        /** SF `TeamMemberSafetyCheckController.callOut` 본부 영업관리자 단일 본부 코드 — 두 사업소 통합 조회 트리거 */
        private const val SF_HQ_INTEGRATED_COST_CENTER = "3232"

        /** SF `TeamMemberSafetyCheckController.callOut` 본부 통합 조회 대상 사업소 코드 — HRcode 3232 사용자가 통합으로 보는 두 사업소 */
        private val SF_HQ_INTEGRATED_TARGETS = listOf("3234", "3236")
    }
}
