package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Appointment
import org.springframework.data.jpa.repository.JpaRepository

interface AppointmentRepository : JpaRepository<Appointment, Long>
