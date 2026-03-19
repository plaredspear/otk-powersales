package com.otoki.internal.safetycheck.service

import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.safetycheck.dto.request.SafetyCheckSubmitRequest
import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import com.otoki.internal.safetycheck.entity.SafetyCheckSubmission
import com.otoki.internal.safetycheck.exception.AlreadySubmittedException
import com.otoki.internal.safetycheck.exception.RequiredItemsMissingException
import com.otoki.internal.safetycheck.repository.SafetyCheckItemRepository
import com.otoki.internal.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
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
    private lateinit var itemRepository: SafetyCheckItemRepository

    @Mock
    private lateinit var submissionRepository: SafetyCheckSubmissionRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var safetyCheckService: SafetyCheckService

    private val userId = 1L
    private val employeeNumber = "20030117"

    @Nested
    @DisplayName("getChecklistItems - 점검 항목 목록 조회")
    inner class GetChecklistItemsTests {

        @Test
        @DisplayName("정상 조회 - question_num별 그룹화된 항목 반환")
        fun getChecklistItems_success() {
            // Given
            val items = listOf(
                createItem(questionNum = 1, seqNum = 1, contents = "손목보호대를 착용했습니다"),
                createItem(questionNum = 1, seqNum = 2, contents = "숨수건을 소지하고 있습니다"),
                createItem(questionNum = 2, seqNum = 1, contents = "예방사항 1")
            )
            whenever(itemRepository.findByUseYnOrderByQuestionNumAscSeqNumAsc("Y")).thenReturn(items)

            // When
            val result = safetyCheckService.getChecklistItems()

            // Then
            assertThat(result.categories).hasSize(2)

            val section1 = result.categories[0]
            assertThat(section1.questionNum).isEqualTo(1)
            assertThat(section1.title).isEqualTo("안전예방 장비 착용")
            assertThat(section1.inputType).isEqualTo("RADIO")
            assertThat(section1.required).isTrue()
            assertThat(section1.options).containsExactly("예", "해당없음")
            assertThat(section1.items).hasSize(2)
            assertThat(section1.items[0].seqNum).isEqualTo(1)
            assertThat(section1.items[0].contents).isEqualTo("손목보호대를 착용했습니다")

            val section2 = result.categories[1]
            assertThat(section2.questionNum).isEqualTo(2)
            assertThat(section2.title).isEqualTo("안전사고 예방사항")
            assertThat(section2.inputType).isEqualTo("CHECKBOX")
            assertThat(section2.required).isFalse()
            assertThat(section2.options).isNull()
            assertThat(section2.items).hasSize(1)
        }
    }

    @Nested
    @DisplayName("getTodayStatus - 오늘 안전점검 완료 여부 조회")
    inner class GetTodayStatusTests {

        @Test
        @DisplayName("오늘 제출 있음 - completed=true, submittedAt 반환")
        fun getTodayStatus_completed() {
            // Given
            val completeTime = LocalDateTime.now().minusHours(2)
            val submission = createSubmission(completeTime = completeTime)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
            whenever(submissionRepository.findByEmployeeIdAndWorkingDate(userId, LocalDate.now()))
                .thenReturn(Optional.of(submission))

            // When
            val result = safetyCheckService.getTodayStatus(userId)

            // Then
            assertThat(result.completed).isTrue()
            assertThat(result.submittedAt).isEqualTo(completeTime)
        }

        @Test
        @DisplayName("오늘 제출 없음 - completed=false, submittedAt=null")
        fun getTodayStatus_notCompleted() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
            whenever(submissionRepository.findByEmployeeIdAndWorkingDate(userId, LocalDate.now()))
                .thenReturn(Optional.empty())

            // When
            val result = safetyCheckService.getTodayStatus(userId)

            // Then
            assertThat(result.completed).isFalse()
            assertThat(result.submittedAt).isNull()
        }

        @Test
        @DisplayName("사용자 없음 - UserNotFoundException")
        fun getTodayStatus_userNotFound() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { safetyCheckService.getTodayStatus(userId) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("submitSafetyCheck - 안전점검 제출")
    inner class SubmitSafetyCheckTests {

        @Test
        @DisplayName("정상 제출 - 9개 장비 응답 + 예방사항 2건")
        fun submitSafetyCheck_success() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
            whenever(submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()))
                .thenReturn(false)
            whenever(itemRepository.countByQuestionNumAndUseYn(1, "Y")).thenReturn(9L)
            whenever(submissionRepository.save(any<SafetyCheckSubmission>()))
                .thenAnswer { it.getArgument<SafetyCheckSubmission>(0) }

            val request = createSubmitRequest()

            // When
            val result = safetyCheckService.submitSafetyCheck(userId, request)

            // Then
            assertThat(result.safetyCheckCompleted).isTrue()
            assertThat(result.submittedAt).isEqualTo(request.completeTime)
        }

        @Test
        @DisplayName("예방사항 없이 제출 - precautions 빈 배열")
        fun submitSafetyCheck_noPrecautions() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
            whenever(submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()))
                .thenReturn(false)
            whenever(itemRepository.countByQuestionNumAndUseYn(1, "Y")).thenReturn(9L)
            whenever(submissionRepository.save(any<SafetyCheckSubmission>()))
                .thenAnswer { it.getArgument<SafetyCheckSubmission>(0) }

            val request = createSubmitRequest(precautions = emptyList())

            // When
            val result = safetyCheckService.submitSafetyCheck(userId, request)

            // Then
            assertThat(result.safetyCheckCompleted).isTrue()
        }

        @Test
        @DisplayName("중복 제출 - AlreadySubmittedException")
        fun submitSafetyCheck_alreadySubmitted() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
            whenever(submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()))
                .thenReturn(true)

            // When & Then
            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, createSubmitRequest()) }
                .isInstanceOf(AlreadySubmittedException::class.java)
        }

        @Test
        @DisplayName("장비 응답 수 불일치 - 8개 제출 시 RequiredItemsMissingException")
        fun submitSafetyCheck_equipmentCountMismatch() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
            whenever(submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()))
                .thenReturn(false)
            whenever(itemRepository.countByQuestionNumAndUseYn(1, "Y")).thenReturn(9L)

            val request = createSubmitRequest(equipmentCount = 8)

            // When & Then
            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, request) }
                .isInstanceOf(RequiredItemsMissingException::class.java)
        }

        @Test
        @DisplayName("잘못된 응답값 - answer가 '아니오'인 경우 RequiredItemsMissingException")
        fun submitSafetyCheck_invalidAnswer() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
            whenever(submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()))
                .thenReturn(false)
            whenever(itemRepository.countByQuestionNumAndUseYn(1, "Y")).thenReturn(9L)

            val equipments = (1..9).map { seqNum ->
                SafetyCheckSubmitRequest.EquipmentAnswer(
                    seqNum = seqNum,
                    answer = if (seqNum == 5) "아니오" else "예"
                )
            }
            val request = SafetyCheckSubmitRequest(
                startTime = LocalDateTime.now(),
                completeTime = LocalDateTime.now(),
                equipments = equipments,
                precautions = null
            )

            // When & Then
            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, request) }
                .isInstanceOf(RequiredItemsMissingException::class.java)
        }

        @Test
        @DisplayName("사용자 없음 - UserNotFoundException")
        fun submitSafetyCheck_userNotFound() {
            // Given
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, createSubmitRequest()) }
                .isInstanceOf(UserNotFoundException::class.java)
        }
    }

    private fun createUser(
        id: Long = userId,
        employeeNumber: String = this.employeeNumber,
        name: String = "테스트 사용자"
    ): User {
        return User(id = id, employeeNumber = employeeNumber, name = name)
    }

    private fun createItem(
        questionNum: Int = 1,
        seqNum: Int = 1,
        contents: String = "테스트 항목",
        useYn: String = "Y"
    ): SafetyCheckItem {
        return SafetyCheckItem(
            questionNum = questionNum,
            seqNum = seqNum,
            contents = contents,
            useYn = useYn
        )
    }

    private fun createSubmission(
        masterId: String = "",
        employeeId: Long = userId,
        workingDate: LocalDate = LocalDate.now(),
        completeTime: LocalDateTime? = LocalDateTime.now()
    ): SafetyCheckSubmission {
        return SafetyCheckSubmission(
            masterId = masterId,
            employeeId = employeeId,
            workingDate = workingDate,
            completeTime = completeTime
        )
    }

    private fun createSubmitRequest(
        equipmentCount: Int = 9,
        precautions: List<String>? = listOf("예방사항 1", "예방사항 3")
    ): SafetyCheckSubmitRequest {
        val equipments = (1..equipmentCount).map { seqNum ->
            SafetyCheckSubmitRequest.EquipmentAnswer(
                seqNum = seqNum,
                answer = if (seqNum % 3 == 0) "해당없음" else "예"
            )
        }
        return SafetyCheckSubmitRequest(
            startTime = LocalDateTime.of(2026, 3, 15, 9, 0, 0),
            completeTime = LocalDateTime.of(2026, 3, 15, 9, 2, 30),
            equipments = equipments,
            precautions = precautions
        )
    }
}
