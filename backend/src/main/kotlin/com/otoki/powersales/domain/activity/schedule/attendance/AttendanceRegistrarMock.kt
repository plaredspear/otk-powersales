package com.otoki.powersales.domain.activity.schedule.attendance

import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.activity.schedule.repository.AttendanceLogRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 출근 등록 Mock 구현
 *
 * 실 연동 전까지 사용하는 Mock. `attendance_log` 더미 row 를 INSERT 해 반환한다.
 * TMS 백링크·안전점검 stamp 는 실 구현과 동일하게 호출측(AttendanceService)이 managed entity 에 반영한다.
 *
 * Spec #789 정합: 신규 시스템 비즈니스 로직 가드는 attendance_log_id (id-FK) 기준. mock 도 동일 패턴으로 시뮬레이션.
 */
@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = ["attendance.registrar.mock.enabled"], havingValue = "true", matchIfMissing = false)
class AttendanceRegistrarMock(
    private val attendanceLogRepository: AttendanceLogRepository,
) : AttendanceRegistrar {

    @Transactional
    override fun register(request: AttendanceRegisterRequest): AttendanceLog {
        return attendanceLogRepository.save(AttendanceLog())
    }
}
