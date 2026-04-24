package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.Appointment
import org.springframework.data.jpa.repository.JpaRepository

interface AppointmentRepository : JpaRepository<Appointment, Long> {

    fun findFirstByEmployeeCodeOrderByAppointDateDesc(employeeCode: String): Appointment?
}
