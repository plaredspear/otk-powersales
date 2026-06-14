package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendanceLogSearchRequest
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceLogNotFoundException
import com.otoki.powersales.domain.activity.schedule.repository.AttendanceLogRepository
import com.otoki.powersales.domain.activity.schedule.service.AdminAttendanceLogService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("AdminAttendanceLogService 테스트")
class AdminAttendanceLogServiceTest {

    private val attendanceLogRepository: AttendanceLogRepository = mockk()
    private val service = AdminAttendanceLogService(attendanceLogRepository)

    @Test
    @DisplayName("search - Repository 결과를 ListItem 로 매핑")
    fun search_mapsResults() {
        val log = AttendanceLog(
            id = 1L,
            name = "AL-001",
            employeeId = 10L,
            accountId = 20,
            attendanceDate = LocalDateTime.of(2026, 5, 18, 9, 0),
            attendanceType = AttendanceType.REGULAR,
            reason = "정상 출근",
        )
        val filter = AdminAttendanceLogSearchRequest()
        val pageable = PageRequest.of(0, 20)
        every { attendanceLogRepository.searchByFilter(eq(filter), any()) } returns
            PageImpl(listOf(log), pageable, 1)

        val result = service.search(filter, pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].id).isEqualTo(1L)
        assertThat(result.content[0].name).isEqualTo("AL-001")
        assertThat(result.content[0].attendanceType).isEqualTo(AttendanceType.REGULAR)
        assertThat(result.content[0].reason).isEqualTo("정상 출근")
    }

    @Test
    @DisplayName("get - id 미존재 시 AttendanceLogNotFoundException")
    fun get_missing_throws() {
        every { attendanceLogRepository.findById(999L) } returns Optional.empty()

        assertThatThrownBy { service.get(999L) }
            .isInstanceOf(AttendanceLogNotFoundException::class.java)
    }

    @Test
    @DisplayName("get - 정상 조회 후 Detail 반환")
    fun get_success() {
        val log = AttendanceLog(
            id = 5L,
            name = "AL-005",
            sfid = "001000000000005",
            attendanceType = AttendanceType.DISPLAY,
        )
        every { attendanceLogRepository.findById(5L) } returns Optional.of(log)

        val detail = service.get(5L)

        assertThat(detail.id).isEqualTo(5L)
        // sfid 는 SF 데이터 마이그레이션 보조 필드 — API 응답 DTO 에서 제거됨 (정책).
        assertThat(detail.attendanceType).isEqualTo(AttendanceType.DISPLAY)
    }

    @Test
    @DisplayName("normalizePageable - MAX_PAGE_SIZE 초과 시 잘림")
    fun normalizePageable_clamps() {
        val original = PageRequest.of(0, 500)
        val normalized = AdminAttendanceLogService.normalizePageable(original)

        assertThat(normalized.pageSize).isEqualTo(AdminAttendanceLogService.MAX_PAGE_SIZE)
    }
}
