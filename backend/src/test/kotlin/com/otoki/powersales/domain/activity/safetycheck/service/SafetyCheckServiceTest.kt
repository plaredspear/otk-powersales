package com.otoki.powersales.domain.activity.safetycheck.service

import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.safetycheck.dto.request.SafetyCheckSubmitRequest
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckItem
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.domain.activity.safetycheck.exception.AlreadySubmittedException
import com.otoki.powersales.domain.activity.safetycheck.exception.RequiredItemsMissingException
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckItemRepository
import com.otoki.powersales.domain.activity.safetycheck.service.SafetyCheckService
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional

@DisplayName("SafetyCheckService 테스트")
class SafetyCheckServiceTest {

    private val itemRepository: SafetyCheckItemRepository = mockk()
    private val submissionRepository: SafetyCheckSubmissionRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val safetyCheckService = SafetyCheckService(
        itemRepository,
        submissionRepository,
        employeeRepository,
    )

    private val userId = 1L
    private val employeeCode = "20030117"

    @Nested
    @DisplayName("getChecklistItems - 점검 항목 목록 조회")
    inner class GetChecklistItemsTests {

        @Test
        @DisplayName("정상 조회 - question_num별 그룹화된 항목 반환")
        fun getChecklistItems_success() {
            val items = listOf(
                createItem(questionNum = 1, seqNum = 1, contents = "손목보호대를 착용했습니다"),
                createItem(questionNum = 1, seqNum = 2, contents = "숨수건을 소지하고 있습니다"),
                createItem(questionNum = 2, seqNum = 1, contents = "예방사항 1")
            )
            every { itemRepository.findByUseYnOrderByQuestionNumAscSeqNumAsc("Y") } returns items

            val result = safetyCheckService.getChecklistItems()

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
            val completeTime = LocalDateTime.now().minus(2, ChronoUnit.HOURS)
            val submission = createSubmission(completeTime = completeTime)
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { submissionRepository.findByEmployeeIdAndWorkingDate(userId, LocalDate.now()) } returns
                Optional.of(submission)

            val result = safetyCheckService.getTodayStatus(userId)

            assertThat(result.completed).isTrue()
            assertThat(result.submittedAt).isEqualTo(completeTime)
        }

        @Test
        @DisplayName("오늘 제출 없음 - completed=false, submittedAt=null")
        fun getTodayStatus_notCompleted() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { submissionRepository.findByEmployeeIdAndWorkingDate(userId, LocalDate.now()) } returns
                Optional.empty()

            val result = safetyCheckService.getTodayStatus(userId)

            assertThat(result.completed).isFalse()
            assertThat(result.submittedAt).isNull()
        }

        @Test
        @DisplayName("사용자 없음 - EmployeeNotFoundException")
        fun getTodayStatus_userNotFound() {
            every { employeeRepository.findById(userId) } returns Optional.empty()

            assertThatThrownBy { safetyCheckService.getTodayStatus(userId) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("submitSafetyCheck - 안전점검 제출")
    inner class SubmitSafetyCheckTests {

        @Test
        @DisplayName("정상 제출 - 9개 장비 응답 + 예방사항 2건")
        fun submitSafetyCheck_success() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()) } returns false
            every { itemRepository.countByQuestionNumAndUseYn(1, "Y") } returns 9L
            every { submissionRepository.save(any<SafetyCheckSubmission>()) } answers { firstArg() }

            val request = createSubmitRequest()

            val result = safetyCheckService.submitSafetyCheck(userId, request)

            assertThat(result.safetyCheckCompleted).isTrue()
            assertThat(result.submittedAt).isEqualTo(request.completeTime)
        }

        @Test
        @DisplayName("예방사항 없이 제출 - precautions 빈 배열")
        fun submitSafetyCheck_noPrecautions() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()) } returns false
            every { itemRepository.countByQuestionNumAndUseYn(1, "Y") } returns 9L
            every { submissionRepository.save(any<SafetyCheckSubmission>()) } answers { firstArg() }

            val request = createSubmitRequest(precautions = emptyList())

            val result = safetyCheckService.submitSafetyCheck(userId, request)

            assertThat(result.safetyCheckCompleted).isTrue()
        }

        @Test
        @DisplayName("중복 제출 - AlreadySubmittedException")
        fun submitSafetyCheck_alreadySubmitted() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()) } returns true

            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, createSubmitRequest()) }
                .isInstanceOf(AlreadySubmittedException::class.java)
        }

        @Test
        @DisplayName("장비 응답 수 불일치 - 8개 제출 시 RequiredItemsMissingException")
        fun submitSafetyCheck_equipmentCountMismatch() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()) } returns false
            every { itemRepository.countByQuestionNumAndUseYn(1, "Y") } returns 9L

            val request = createSubmitRequest(equipmentCount = 8)

            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, request) }
                .isInstanceOf(RequiredItemsMissingException::class.java)
        }

        @Test
        @DisplayName("잘못된 응답값 - answer가 '아니오'인 경우 RequiredItemsMissingException")
        fun submitSafetyCheck_invalidAnswer() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { submissionRepository.existsByEmployeeIdAndWorkingDate(userId, LocalDate.now()) } returns false
            every { itemRepository.countByQuestionNumAndUseYn(1, "Y") } returns 9L

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

            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, request) }
                .isInstanceOf(RequiredItemsMissingException::class.java)
        }

        @Test
        @DisplayName("사용자 없음 - EmployeeNotFoundException")
        fun submitSafetyCheck_userNotFound() {
            every { employeeRepository.findById(userId) } returns Optional.empty()

            assertThatThrownBy { safetyCheckService.submitSafetyCheck(userId, createSubmitRequest()) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    private fun createEmployee(
        id: Long = userId,
        employeeCode: String = this.employeeCode,
        name: String = "테스트 사용자"
    ): Employee {
        return Employee(id = id, employeeCode = employeeCode, name = name)
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
        displayWorkScheduleId: Long? = null,
        employeeId: Long = userId,
        workingDate: LocalDate = LocalDate.now(),
        completeTime: LocalDateTime? = LocalDateTime.now()
    ): SafetyCheckSubmission {
        return SafetyCheckSubmission(
            displayWorkScheduleId = displayWorkScheduleId,
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
