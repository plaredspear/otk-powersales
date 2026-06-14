package com.otoki.powersales.domain.activity.safetycheck.service

import com.otoki.powersales.domain.activity.safetycheck.dto.request.SafetyCheckSubmitRequest
import com.otoki.powersales.domain.activity.safetycheck.dto.response.SafetyCheckItemsResponse
import com.otoki.powersales.domain.activity.safetycheck.dto.response.SafetyCheckSubmitResponse
import com.otoki.powersales.domain.activity.safetycheck.dto.response.SafetyCheckTodayResponse
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.domain.activity.safetycheck.exception.AlreadySubmittedException
import com.otoki.powersales.domain.activity.safetycheck.exception.RequiredItemsMissingException
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckItemRepository
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class SafetyCheckService(
    private val itemRepository: SafetyCheckItemRepository,
    private val submissionRepository: SafetyCheckSubmissionRepository,
    private val employeeRepository: EmployeeRepository
) {

    fun getChecklistItems(): SafetyCheckItemsResponse {
        val items = itemRepository.findByUseYnOrderByQuestionNumAscSeqNumAsc("Y")
        val grouped = items.groupBy { it.questionNum }

        val categories = grouped.map { (questionNum, groupItems) ->
            SafetyCheckItemsResponse.CategoryInfo(
                questionNum = questionNum,
                title = if (questionNum == 1) "안전예방 장비 착용" else "안전사고 예방사항",
                inputType = if (questionNum == 1) "RADIO" else "CHECKBOX",
                required = questionNum == 1,
                options = if (questionNum == 1) listOf("예", "해당없음") else null,
                items = groupItems.map { item ->
                    SafetyCheckItemsResponse.CheckItemInfo(
                        seqNum = item.seqNum,
                        contents = item.contents
                    )
                }
            )
        }

        return SafetyCheckItemsResponse(categories = categories)
    }

    fun getTodayStatus(userId: Long): SafetyCheckTodayResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        val today = LocalDate.now()
        val submission = submissionRepository.findByEmployeeIdAndWorkingDate(employee.id, today)

        return if (submission.isPresent) {
            SafetyCheckTodayResponse(
                completed = true,
                submittedAt = submission.get().completeTime
            )
        } else {
            SafetyCheckTodayResponse(
                completed = false,
                submittedAt = null
            )
        }
    }

    @Transactional
    fun submitSafetyCheck(userId: Long, request: SafetyCheckSubmitRequest): SafetyCheckSubmitResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        val today = LocalDate.now()

        if (submissionRepository.existsByEmployeeIdAndWorkingDate(employee.id, today)) {
            throw AlreadySubmittedException()
        }

        val activeEquipmentCount = itemRepository.countByQuestionNumAndUseYn(1, "Y")
        if (request.equipments.size.toLong() != activeEquipmentCount) {
            throw RequiredItemsMissingException()
        }

        val validAnswers = setOf("예", "해당없음")
        if (request.equipments.any { it.answer !in validAnswers }) {
            throw RequiredItemsMissingException()
        }

        val sortedEquipments = request.equipments.sortedBy { it.seqNum }
        val yesCount = sortedEquipments.count { it.answer == "예" }
        val noCount = sortedEquipments.count { it.answer == "해당없음" }

        val precautionText = request.precautions
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(";")
        val precautionCount = request.precautions?.size ?: 0

        val submission = SafetyCheckSubmission(
            employeeId = employee.id,
            workingDate = today,
            startTime = request.startTime,
            completeTime = request.completeTime,
            yesCheckCount = yesCount,
            noCheckCount = noCount,
            equipment1 = sortedEquipments.getOrNull(0)?.answer,
            equipment2 = sortedEquipments.getOrNull(1)?.answer,
            equipment3 = sortedEquipments.getOrNull(2)?.answer,
            equipment4 = sortedEquipments.getOrNull(3)?.answer,
            equipment5 = sortedEquipments.getOrNull(4)?.answer,
            equipment6 = sortedEquipments.getOrNull(5)?.answer,
            equipment7 = sortedEquipments.getOrNull(6)?.answer,
            equipment8 = sortedEquipments.getOrNull(7)?.answer,
            equipment9 = sortedEquipments.getOrNull(8)?.answer,
            precaution = precautionText,
            precautionCheckCount = precautionCount,
            traversalFlag = "O",
            completeWorkYn = "N"
        )

        submissionRepository.save(submission)

        return SafetyCheckSubmitResponse(
            submittedAt = request.completeTime,
            safetyCheckCompleted = true
        )
    }
}
