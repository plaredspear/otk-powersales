package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import org.springframework.data.jpa.repository.JpaRepository

interface AppointmentRepository : JpaRepository<Appointment, Long> {

    fun findFirstByEmployeeCodeOrderByAppointDateDesc(employeeCode: String): Appointment?
}
