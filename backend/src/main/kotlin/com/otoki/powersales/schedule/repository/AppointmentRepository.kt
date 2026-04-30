package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.Appointment
import org.springframework.data.jpa.repository.JpaRepository

interface AppointmentRepository : JpaRepository<Appointment, Long> {

    fun findFirstByEmployeeCodeOrderByAppointDateDesc(employeeCode: String): Appointment?
}
