package com.otoki.powersales.agreement.service

import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.common.repository.AgreementWordRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * AgreementWordCycleService 분기 + cascade reset 테스트 (스펙 #654 / T1~T4 시나리오).
 *
 * `LocalDate.now(SEOUL_ZONE)` 가 service 내부에서 직접 호출되므로 today 를 외부 주입 없이 검증한다.
 * cycle 도메인 분기는 candidates 의 `active` / `activeDate` / `afterActiveDate` 조합으로만 결정되므로
 * candidates 를 SEOUL_ZONE today 기준으로 구성하면 분기 검증에 충분하다.
 */
@DisplayName("AgreementWordCycleService — GPS 재동의 cycle 처리 (#654)")
class AgreementWordCycleServiceTest {

    private val agreementWordRepository: AgreementWordRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val service = AgreementWordCycleService(
        agreementWordRepository,
        employeeRepository,
    )

    private val today: LocalDate = LocalDate.now(com.otoki.powersales.common.util.TimeZones.SEOUL_ZONE)

    @Nested
    @DisplayName("runCycle - 분기 처리")
    inner class RunCycleBranches {

        @Test
        @DisplayName("T1 rotation - 활성 + 도래 둘 다 존재 -> 활성 교대 + cascade reset 발화")
        fun rotation() {
            val oldActive = createAgreementWord(
                id = 1, active = true, activeDate = today.minusMonths(6), afterActiveDate = today.minusMonths(6).plusMonths(6)
            )
            val newDue = createAgreementWord(
                id = 2, active = false, activeDate = null, afterActiveDate = today
            )
            every { agreementWordRepository.findActiveOrDueCandidates(today) } returns
                listOf(oldActive, newDue)
            every { employeeRepository.resetAgreementFlagForActiveConsents() } returns 42L

            val result = service.runCycle()

            assertThat(result.branch).isEqualTo(AgreementWordCycleService.Branch.ROTATION)
            assertThat(result.resetCount).isEqualTo(42L)
            assertThat(oldActive.active).isFalse()
            assertThat(oldActive.afterActiveDate).isNull()
            assertThat(newDue.active).isTrue()
            assertThat(newDue.activeDate).isEqualTo(today)
            assertThat(newDue.afterActiveDate).isEqualTo(today.plusMonths(6))
            verify { employeeRepository.resetAgreementFlagForActiveConsents() }
        }

        @Test
        @DisplayName("T2 new-only - 활성 부재 + 도래만 존재 -> 신규 활성화 + cascade reset 발화")
        fun newOnly() {
            val newDue = createAgreementWord(
                id = 2, active = false, activeDate = null, afterActiveDate = today
            )
            every { agreementWordRepository.findActiveOrDueCandidates(today) } returns listOf(newDue)
            every { employeeRepository.resetAgreementFlagForActiveConsents() } returns 7L

            val result = service.runCycle()

            assertThat(result.branch).isEqualTo(AgreementWordCycleService.Branch.NEW_ONLY)
            assertThat(result.resetCount).isEqualTo(7L)
            assertThat(newDue.active).isTrue()
            assertThat(newDue.activeDate).isEqualTo(today)
            assertThat(newDue.afterActiveDate).isEqualTo(today.plusMonths(6))
            verify { employeeRepository.resetAgreementFlagForActiveConsents() }
        }

        @Test
        @DisplayName("self-renewal - 활성만 존재 + AfterActiveDate=today -> 활성 유지 갱신 + cascade reset 발화 (legacy 동등)")
        fun selfRenewal() {
            val oldActive = createAgreementWord(
                id = 1, active = true, activeDate = today.minusMonths(6), afterActiveDate = today
            )
            every { agreementWordRepository.findActiveOrDueCandidates(today) } returns listOf(oldActive)
            every { employeeRepository.resetAgreementFlagForActiveConsents() } returns 15L

            val result = service.runCycle()

            assertThat(result.branch).isEqualTo(AgreementWordCycleService.Branch.SELF_RENEWAL)
            assertThat(result.resetCount).isEqualTo(15L)
            assertThat(oldActive.active).isTrue()
            assertThat(oldActive.activeDate).isEqualTo(today)
            assertThat(oldActive.afterActiveDate).isEqualTo(today.plusMonths(6))
            verify { employeeRepository.resetAgreementFlagForActiveConsents() }
        }

        @Test
        @DisplayName("T3 no-op - 활성 1건만 + AfterActiveDate≠today -> DML 0건 + cascade 미발화")
        fun noOp() {
            val oldActive = createAgreementWord(
                id = 1, active = true, activeDate = today.minusMonths(3), afterActiveDate = today.plusMonths(3)
            )
            every { agreementWordRepository.findActiveOrDueCandidates(today) } returns listOf(oldActive)

            val result = service.runCycle()

            assertThat(result.branch).isEqualTo(AgreementWordCycleService.Branch.NO_OP)
            assertThat(result.resetCount).isEqualTo(0L)
            assertThat(oldActive.active).isTrue()
            assertThat(oldActive.activeDate).isEqualTo(today.minusMonths(3))
            assertThat(oldActive.afterActiveDate).isEqualTo(today.plusMonths(3))
            verify(exactly = 0) { employeeRepository.resetAgreementFlagForActiveConsents() }
        }

        @Test
        @DisplayName("후보 0건 - DML 0건 + cascade 미발화")
        fun emptyCandidates() {
            every { agreementWordRepository.findActiveOrDueCandidates(today) } returns emptyList()

            val result = service.runCycle()

            assertThat(result.branch).isEqualTo(AgreementWordCycleService.Branch.NO_OP)
            assertThat(result.resetCount).isEqualTo(0L)
            verify(exactly = 0) { employeeRepository.resetAgreementFlagForActiveConsents() }
        }
    }

    @Nested
    @DisplayName("runCycle - cascade reset 동작")
    inner class CascadeReset {

        @Test
        @DisplayName("T4 partial-fail - cascade reset 도중 예외 -> 전체 rollback (Q3 all-or-nothing)")
        fun cascadeFailurePropagates() {
            val newDue = createAgreementWord(id = 2, active = false, activeDate = null, afterActiveDate = today)
            every { agreementWordRepository.findActiveOrDueCandidates(today) } returns listOf(newDue)
            every { employeeRepository.resetAgreementFlagForActiveConsents() } throws
                RuntimeException("DB connection lost")

            org.assertj.core.api.Assertions.assertThatThrownBy { service.runCycle() }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("DB connection lost")
        }

        @Test
        @DisplayName("cascade reset 0건 - 활성 사원 없는 환경에서도 정상 종료")
        fun cascadeZeroRowsOk() {
            val newDue = createAgreementWord(id = 2, active = false, activeDate = null, afterActiveDate = today)
            every { agreementWordRepository.findActiveOrDueCandidates(today) } returns listOf(newDue)
            every { employeeRepository.resetAgreementFlagForActiveConsents() } returns 0L

            val result = service.runCycle()

            assertThat(result.branch).isEqualTo(AgreementWordCycleService.Branch.NEW_ONLY)
            assertThat(result.resetCount).isEqualTo(0L)
        }
    }

    private fun createAgreementWord(
        id: Int,
        active: Boolean,
        activeDate: LocalDate?,
        afterActiveDate: LocalDate?
    ): AgreementWord = AgreementWord(
        id = id,
        name = "동의문구 v$id",
        contents = "내용",
        active = active,
        activeDate = activeDate,
        afterActiveDate = afterActiveDate,
        isDeleted = false
    )
}
