package com.otoki.internal.service

import com.otoki.internal.dto.request.SafetyCheckSubmitRequest
import com.otoki.internal.entity.SafetyCheckCategory
import com.otoki.internal.entity.SafetyCheckItem
import com.otoki.internal.entity.SafetyCheckSubmission
import com.otoki.internal.exception.AlreadySubmittedException
import com.otoki.internal.exception.RequiredItemsMissingException
import com.otoki.internal.repository.SafetyCheckCategoryRepository
import com.otoki.internal.repository.SafetyCheckItemRepository
import com.otoki.internal.repository.SafetyCheckSubmissionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("SafetyCheckService 테스트")
class SafetyCheckServiceTest {

    @Mock
    private lateinit var categoryRepository: SafetyCheckCategoryRepository

    @Mock
    private lateinit var itemRepository: SafetyCheckItemRepository

    @Mock
    private lateinit var submissionRepository: SafetyCheckSubmissionRepository

    @InjectMocks
    private lateinit var safetyCheckService: SafetyCheckService

    // ========== getChecklistItems Tests ==========

    @Nested
    @DisplayName("getChecklistItems - 안전점검 항목 조회")
    inner class GetChecklistItemsTests {

        @Test
        @DisplayName("활성 카테고리와 활성 항목만 반환한다")
        fun getChecklistItems_onlyActiveItems() {
            // Given
            val category1 = createCategory(id = 1L, name = "안전예방 장비 착용", sortOrder = 1)
            val category2 = createCategory(id = 2L, name = "사고 예방", sortOrder = 2)

            val items1 = listOf(
                createItem(id = 1L, category = category1, label = "손목보호대", sortOrder = 1),
                createItem(id = 2L, category = category1, label = "안전화", sortOrder = 2)
            )
            val items2 = listOf(
                createItem(id = 3L, category = category2, label = "지게차 근접 금지", sortOrder = 1)
            )

            whenever(categoryRepository.findByActiveTrueOrderBySortOrderAsc())
                .thenReturn(listOf(category1, category2))
            whenever(itemRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(1L))
                .thenReturn(items1)
            whenever(itemRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(2L))
                .thenReturn(items2)

            // When
            val result = safetyCheckService.getChecklistItems()

            // Then
            assertThat(result.categories).hasSize(2)
            assertThat(result.categories[0].name).isEqualTo("안전예방 장비 착용")
            assertThat(result.categories[0].items).hasSize(2)
            assertThat(result.categories[0].items[0].label).isEqualTo("손목보호대")
            assertThat(result.categories[0].items[1].label).isEqualTo("안전화")
            assertThat(result.categories[1].name).isEqualTo("사고 예방")
            assertThat(result.categories[1].items).hasSize(1)
            assertThat(result.categories[1].items[0].label).isEqualTo("지게차 근접 금지")
        }

        @Test
        @DisplayName("항목이 sortOrder 기준으로 정렬된다")
        fun getChecklistItems_sortedBySortOrder() {
            // Given
            val category = createCategory(id = 1L, name = "테스트", sortOrder = 1)
            val items = listOf(
                createItem(id = 1L, category = category, label = "첫번째", sortOrder = 1),
                createItem(id = 2L, category = category, label = "두번째", sortOrder = 2),
                createItem(id = 3L, category = category, label = "세번째", sortOrder = 3)
            )

            whenever(categoryRepository.findByActiveTrueOrderBySortOrderAsc())
                .thenReturn(listOf(category))
            whenever(itemRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(1L))
                .thenReturn(items)

            // When
            val result = safetyCheckService.getChecklistItems()

            // Then
            assertThat(result.categories[0].items).hasSize(3)
            assertThat(result.categories[0].items[0].sortOrder).isEqualTo(1)
            assertThat(result.categories[0].items[1].sortOrder).isEqualTo(2)
            assertThat(result.categories[0].items[2].sortOrder).isEqualTo(3)
        }

        @Test
        @DisplayName("카테고리가 없으면 빈 목록을 반환한다")
        fun getChecklistItems_emptyWhenNoCategories() {
            // Given
            whenever(categoryRepository.findByActiveTrueOrderBySortOrderAsc())
                .thenReturn(emptyList())

            // When
            val result = safetyCheckService.getChecklistItems()

            // Then
            assertThat(result.categories).isEmpty()
        }

        @Test
        @DisplayName("카테고리의 description이 null일 수 있다")
        fun getChecklistItems_nullDescription() {
            // Given
            val category = SafetyCheckCategory(
                id = 1L,
                name = "테스트",
                description = null,
                sortOrder = 1
            )

            whenever(categoryRepository.findByActiveTrueOrderBySortOrderAsc())
                .thenReturn(listOf(category))
            whenever(itemRepository.findByCategoryIdAndActiveTrueOrderBySortOrderAsc(1L))
                .thenReturn(emptyList())

            // When
            val result = safetyCheckService.getChecklistItems()

            // Then
            assertThat(result.categories[0].description).isNull()
        }
    }

    // ========== submitSafetyCheck Tests ==========

    @Nested
    @DisplayName("submitSafetyCheck - 안전점검 제출")
    inner class SubmitSafetyCheckTests {

        @Test
        @DisplayName("정상 제출 - 모든 필수 항목 체크 시 제출 성공")
        fun submitSafetyCheck_success() {
            // Given
            val userId = 1L
            val category = createCategory(id = 1L, name = "테스트", sortOrder = 1)
            val requiredItems = listOf(
                createItem(id = 1L, category = category, label = "항목1", sortOrder = 1, required = true),
                createItem(id = 2L, category = category, label = "항목2", sortOrder = 2, required = true)
            )
            val request = SafetyCheckSubmitRequest(checkedItemIds = listOf(1L, 2L, 3L))

            whenever(submissionRepository.existsByUserIdAndSubmissionDate(userId, LocalDate.now()))
                .thenReturn(false)
            whenever(itemRepository.findByRequiredTrueAndActiveTrue())
                .thenReturn(requiredItems)
            whenever(itemRepository.findAllById(listOf(1L, 2L, 3L)))
                .thenReturn(requiredItems)
            whenever(submissionRepository.save(any<SafetyCheckSubmission>()))
                .thenAnswer { invocation ->
                    val submission = invocation.getArgument<SafetyCheckSubmission>(0)
                    SafetyCheckSubmission(
                        id = 123L,
                        userId = submission.userId,
                        submissionDate = submission.submissionDate,
                        submittedAt = submission.submittedAt,
                        submissionItems = submission.submissionItems
                    )
                }

            // When
            val result = safetyCheckService.submitSafetyCheck(userId, request)

            // Then
            assertThat(result.submissionId).isEqualTo(123L)
            assertThat(result.safetyCheckCompleted).isTrue()
            assertThat(result.submittedAt).isNotNull()
        }

        @Test
        @DisplayName("중복 제출 - 오늘 이미 제출한 경우 AlreadySubmittedException 발생")
        fun submitSafetyCheck_alreadySubmitted() {
            // Given
            val userId = 1L
            val request = SafetyCheckSubmitRequest(checkedItemIds = listOf(1L, 2L))

            whenever(submissionRepository.existsByUserIdAndSubmissionDate(userId, LocalDate.now()))
                .thenReturn(true)

            // When & Then
            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, request) }
                .isInstanceOf(AlreadySubmittedException::class.java)
        }

        @Test
        @DisplayName("필수 항목 누락 - 일부 필수 항목만 체크 시 RequiredItemsMissingException 발생")
        fun submitSafetyCheck_requiredItemsMissing() {
            // Given
            val userId = 1L
            val category = createCategory(id = 1L, name = "테스트", sortOrder = 1)
            val requiredItems = listOf(
                createItem(id = 1L, category = category, label = "항목1", sortOrder = 1, required = true),
                createItem(id = 2L, category = category, label = "항목2", sortOrder = 2, required = true),
                createItem(id = 3L, category = category, label = "항목3", sortOrder = 3, required = true)
            )
            // 필수 항목 3개 중 2개만 체크
            val request = SafetyCheckSubmitRequest(checkedItemIds = listOf(1L, 2L))

            whenever(submissionRepository.existsByUserIdAndSubmissionDate(userId, LocalDate.now()))
                .thenReturn(false)
            whenever(itemRepository.findByRequiredTrueAndActiveTrue())
                .thenReturn(requiredItems)

            // When & Then
            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, request) }
                .isInstanceOf(RequiredItemsMissingException::class.java)
        }
    }

    // ========== getTodayStatus Tests ==========

    @Nested
    @DisplayName("getTodayStatus - 오늘 안전점검 완료 여부 조회")
    inner class GetTodayStatusTests {

        @Test
        @DisplayName("오늘 제출 기록이 있으면 completed=true, submittedAt 포함")
        fun getTodayStatus_completed() {
            // Given
            val userId = 1L
            val submittedAt = LocalDateTime.now().minusHours(2)
            val submission = SafetyCheckSubmission(
                id = 1L,
                userId = userId,
                submissionDate = LocalDate.now(),
                submittedAt = submittedAt
            )

            whenever(submissionRepository.findByUserIdAndSubmissionDate(userId, LocalDate.now()))
                .thenReturn(Optional.of(submission))

            // When
            val result = safetyCheckService.getTodayStatus(userId)

            // Then
            assertThat(result.completed).isTrue()
            assertThat(result.submittedAt).isEqualTo(submittedAt)
        }

        @Test
        @DisplayName("오늘 제출 기록이 없으면 completed=false, submittedAt=null")
        fun getTodayStatus_notCompleted() {
            // Given
            val userId = 1L

            whenever(submissionRepository.findByUserIdAndSubmissionDate(userId, LocalDate.now()))
                .thenReturn(Optional.empty())

            // When
            val result = safetyCheckService.getTodayStatus(userId)

            // Then
            assertThat(result.completed).isFalse()
            assertThat(result.submittedAt).isNull()
        }
    }

    // ========== Helpers ==========

    private fun createCategory(
        id: Long = 1L,
        name: String = "테스트 카테고리",
        description: String? = "설명",
        sortOrder: Int = 1,
        active: Boolean = true
    ): SafetyCheckCategory {
        return SafetyCheckCategory(
            id = id,
            name = name,
            description = description,
            sortOrder = sortOrder,
            active = active
        )
    }

    private fun createItem(
        id: Long = 1L,
        category: SafetyCheckCategory,
        label: String = "테스트 항목",
        sortOrder: Int = 1,
        required: Boolean = true,
        active: Boolean = true
    ): SafetyCheckItem {
        return SafetyCheckItem(
            id = id,
            category = category,
            label = label,
            sortOrder = sortOrder,
            required = required,
            active = active
        )
    }
}
