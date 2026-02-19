package com.otoki.internal.service

import com.otoki.internal.dto.request.SafetyCheckSubmitRequest
import com.otoki.internal.dto.response.SafetyCheckItemsResponse
import com.otoki.internal.dto.response.SafetyCheckSubmitResponse
import com.otoki.internal.dto.response.SafetyCheckTodayResponse
import com.otoki.internal.entity.SafetyCheckSubmission
import com.otoki.internal.exception.AlreadySubmittedException
import com.otoki.internal.exception.RequiredItemsMissingException
// import com.otoki.internal.repository.SafetyCheckCategoryRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.internal.repository.SafetyCheckItemRepository
import com.otoki.internal.repository.SafetyCheckSubmissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SafetyCheckService(
    // private val categoryRepository: SafetyCheckCategoryRepository,  // Phase2: PG 대응 테이블 없음
    private val itemRepository: SafetyCheckItemRepository,
    private val submissionRepository: SafetyCheckSubmissionRepository
) {

    /**
     * 안전점검 체크리스트 항목 조회
     * - 활성 카테고리만 조회
     * - 각 카테고리 내 활성 항목만 조회
     * - sortOrder 기준 정렬
     */
    // Phase2: SafetyCheckCategory PG 대응 테이블 없음 - getChecklistItems 주석 처리
    // @Transactional(readOnly = true)
    // fun getChecklistItems(): SafetyCheckItemsResponse {
    //     val categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc()
    //     val categoryInfos = categories.map { category ->
    //         val activeItems = itemRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(category.id)
    //         SafetyCheckItemsResponse.CategoryInfo(
    //             id = category.id,
    //             name = category.name,
    //             description = category.description,
    //             items = activeItems.map { item ->
    //                 SafetyCheckItemsResponse.CheckItemInfo(
    //                     id = item.id,
    //                     label = item.label,
    //                     sortOrder = item.sortOrder,
    //                     required = item.required
    //                 )
    //             }
    //         )
    //     }
    //     return SafetyCheckItemsResponse(categories = categoryInfos)
    // }
    @Transactional(readOnly = true)
    fun getChecklistItems(): SafetyCheckItemsResponse {
        return SafetyCheckItemsResponse(categories = emptyList())
    }

    /**
     * 안전점검 제출
     * 1. 오늘 이미 제출했는지 확인
     * 2. 필수 항목이 모두 체크되었는지 검증
     * 3. 제출 기록 저장
     */
    @Transactional
    fun submitSafetyCheck(userId: Long, request: SafetyCheckSubmitRequest): SafetyCheckSubmitResponse {
        val today = LocalDate.now()

        // 1. 중복 제출 확인
        if (submissionRepository.existsByUserIdAndSubmissionDate(userId, today)) {
            throw AlreadySubmittedException()
        }

        // 2. 필수 항목 검증
        val requiredItems = itemRepository.findByRequiredTrueAndActiveTrue()
        val requiredItemIds = requiredItems.map { it.id }.toSet()
        val checkedItemIds = request.checkedItemIds.toSet()

        if (!checkedItemIds.containsAll(requiredItemIds)) {
            throw RequiredItemsMissingException()
        }

        // 3. 체크된 항목 조회
        val checkedItems = itemRepository.findAllById(request.checkedItemIds)

        // 4. 제출 기록 생성
        val submission = SafetyCheckSubmission(
            userId = userId,
            submissionDate = today
        )

        // 5. 제출 항목 추가
        checkedItems.forEach { item ->
            submission.addItem(item)
        }

        // 6. 저장
        val saved = submissionRepository.save(submission)

        return SafetyCheckSubmitResponse(
            submissionId = saved.id,
            submittedAt = saved.submittedAt,
            safetyCheckCompleted = true
        )
    }

    /**
     * 오늘 안전점검 완료 여부 조회
     */
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
