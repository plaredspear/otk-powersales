package com.otoki.powersales.domain.activity.schedule.attendance

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.repository.AttendanceLogRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@DisplayName("AttendanceRegistrarImpl 테스트 — 출근로그 실체 데이터 적재")
class AttendanceRegistrarImplTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val attendanceLogRepository: AttendanceLogRepository = mockk()

    // 고정 시각 clock — attendance_date 가 등록 시각으로 채워지는지 검증용
    private val fixedInstant = Instant.parse("2026-07-06T02:30:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Seoul"))

    private val registrar = AttendanceRegistrarImpl(
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        attendanceLogRepository = attendanceLogRepository,
        clock = clock,
    )

    @Test
    @DisplayName("attendance_log 에 출근일시/사원/거래처/출근종류/사유가 실체 값으로 적재된다")
    fun `attendance_log 실체 데이터 적재`() {
        // given
        val savedSlot = slot<AttendanceLog>()
        every { attendanceLogRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val request = AttendanceRegisterRequest(
            scheduleId = 100L,
            employeeId = 42L,
            accountId = 7L,
            attendanceType = AttendanceType.DISPLAY,
            reason = "지연 출근",
        )

        // when
        val result = registrar.register(request)

        // then — 저장된 AttendanceLog 실체 값 검증 (레거시 Mock 의 빈 row 회귀 방지)
        val saved = savedSlot.captured
        assertThat(saved.employeeId).isEqualTo(42L)
        assertThat(saved.accountId).isEqualTo(7L)
        assertThat(saved.attendanceType).isEqualTo(AttendanceType.DISPLAY)
        assertThat(saved.reason).isEqualTo("지연 출근")
        // attendance_date = clock 기준 등록 시각 (레거시 CommuteDate = system.now() 동등)
        assertThat(saved.attendanceDate).isEqualTo(LocalDateTime.now(clock))

        // TMS 백링크 + 안전점검 반영 호출 확인
        verify { teamMemberScheduleRepository.updateAttendanceLog(100L, saved.id) }
        verify {
            teamMemberScheduleRepository.updateSafetyCheckData(
                id = 100L,
                equipment1 = any(), equipment2 = any(), equipment3 = any(),
                equipment4 = any(), equipment5 = any(), equipment6 = any(),
                equipment7 = any(), equipment8 = any(), equipment9 = any(),
                yesChkCnt = any(), noChkCnt = any(),
                startTime = any(), completeTime = any(),
                precaution = any(), precautionChk = any(), traversalFlag = any(),
            )
        }

        assertThat(result.resultCode).isEqualTo("200")
        assertThat(result.resultMessage).isEqualTo("SUCCESS")
    }

    @Test
    @DisplayName("REGULAR 기본값 — attendanceType 미지정 시 REGULAR 로 적재")
    fun `attendanceType 기본값 REGULAR`() {
        val savedSlot = slot<AttendanceLog>()
        every { attendanceLogRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        registrar.register(AttendanceRegisterRequest(scheduleId = 1L, employeeId = 1L, accountId = 1L))

        assertThat(savedSlot.captured.attendanceType).isEqualTo(AttendanceType.REGULAR)
    }
}
