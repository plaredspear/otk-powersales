package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import org.springframework.data.jpa.repository.JpaRepository

interface AppointmentRepository : JpaRepository<Appointment, Long> {

    /**
     * 사원의 최신 발령 1건.
     *
     * `appoint_date` 단독 정렬은 동일 발령일 다건에서 순서가 비결정적이라, 같은 날짜의 옛 발령을
     * 집어 인사정보를 옛 값으로 되돌리는 사고가 발생한다 (실사례: 2025-05-01 발령 12건 중 직무코드
     * A049 행이 선택되어 A034 승진이 미반영). `created_at` 2차 정렬로 동일 발령일에서는 가장 나중에
     * 적재된 행을 집는다.
     */
    fun findFirstByEmployeeCodeOrderByAppointDateDescCreatedAtDesc(employeeCode: String): Appointment?
}
