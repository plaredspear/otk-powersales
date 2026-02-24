package com.otoki.internal.safetycheck.service

// Phase2: Entity V1 리매핑으로 인해 비즈니스 로직 전체 주석 처리
// V1 비즈니스 로직 복원은 후속 스펙에서 별도 진행

/*
import com.otoki.internal.safetycheck.entity.SafetyCheckSubmission
import com.otoki.internal.safetycheck.exception.AlreadySubmittedException
import com.otoki.internal.safetycheck.exception.RequiredItemsMissingException
import com.otoki.internal.safetycheck.repository.SafetyCheckItemRepository
import com.otoki.internal.safetycheck.repository.SafetyCheckSubmissionRepository
import java.time.LocalDate

@Service
class SafetyCheckService(
    private val itemRepository: SafetyCheckItemRepository,
    private val submissionRepository: SafetyCheckSubmissionRepository
) {

    @Transactional
    fun submitSafetyCheck(userId: Long, request: SafetyCheckSubmitRequest): SafetyCheckSubmitResponse {
        val today = LocalDate.now()

        if (submissionRepository.existsByUserIdAndSubmissionDate(userId, today)) {
            throw AlreadySubmittedException()
        }

        val requiredItems = itemRepository.findByRequiredTrueAndActiveTrue()
        val requiredItemIds = requiredItems.map { it.id }.toSet()
        val checkedItemIds = request.checkedItemIds.toSet()

        if (!checkedItemIds.containsAll(requiredItemIds)) {
            throw RequiredItemsMissingException()
        }

        val checkedItems = itemRepository.findAllById(request.checkedItemIds)

        val submission = SafetyCheckSubmission(
            userId = userId,
            submissionDate = today
        )

        checkedItems.forEach { item ->
            submission.addItem(item)
        }

        val saved = submissionRepository.save(submission)

        return SafetyCheckSubmitResponse(
            submissionId = saved.id,
            submittedAt = saved.submittedAt,
            safetyCheckCompleted = true
        )
    }

    @Transactional(readOnly = true)
    fun getTodayStatus(userId: Long): SafetyCheckTodayResponse {
        val today = LocalDate.now()
        val submission = submissionRepository.findByUserIdAndSubmissionDate(userId, today)

        return if (submission.isPresent) {
            SafetyCheckTodayResponse(
                completed = true,
                submittedAt = submission.get().submittedAt
            )
        } else {
            SafetyCheckTodayResponse(
                completed = false,
                submittedAt = null
            )
        }
    }
}
*/

import com.otoki.internal.safetycheck.dto.request.SafetyCheckSubmitRequest
import com.otoki.internal.safetycheck.dto.response.SafetyCheckItemsResponse
import com.otoki.internal.safetycheck.dto.response.SafetyCheckSubmitResponse
import com.otoki.internal.safetycheck.dto.response.SafetyCheckTodayResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SafetyCheckService {

    // Phase2: 최소 스텁 — Controller 컴파일 유지용

    @Transactional(readOnly = true)
    fun getChecklistItems(): SafetyCheckItemsResponse {
        return SafetyCheckItemsResponse(categories = emptyList())
    }

    @Transactional
    fun submitSafetyCheck(userId: Long, request: SafetyCheckSubmitRequest): SafetyCheckSubmitResponse {
        // Phase2: Entity 리매핑 완료 후 V1 로직 복원 예정
        return SafetyCheckSubmitResponse(
            submissionId = 0L,
            submittedAt = LocalDateTime.now(),
            safetyCheckCompleted = false
        )
    }

    @Transactional(readOnly = true)
    fun getTodayStatus(userId: Long): SafetyCheckTodayResponse {
        return SafetyCheckTodayResponse(completed = false, submittedAt = null)
    }
}
