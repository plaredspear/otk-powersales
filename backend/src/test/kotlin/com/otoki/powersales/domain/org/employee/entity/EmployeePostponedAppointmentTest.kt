package com.otoki.powersales.domain.org.employee.entity

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.schedule.entity.Appointment
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Spec #738 — Employee.postponedAppointment ↔ Appointment FK R-2 패턴 (R-2 후처리) 검증.
 *
 * #713 v2.1 까지의 self-ref 매핑 오류 (DKRetail__Employee__c → Employee) 정정 동반.
 * 실제 referenceTo: Appointment__c → Appointment 엔티티.
 *
 * 검증 분류:
 *   - AC1: postponedAppointment 필드 존재 + @ManyToOne + @JoinColumn(name="postponed_appointment_id")
 *   - AC2: 필드 타입이 Appointment? 인지
 */
@DisplayName("Employee.postponedAppointment FK 검증 (Spec #738)")
class EmployeePostponedAppointmentTest {

    @Test
    @DisplayName("AC1 — postponedAppointment 필드에 @ManyToOne + @JoinColumn(name=postponed_appointment_id) 부착")
    fun postponedAppointmentFieldAnnotations() {
        val field = Employee::class.java.getDeclaredField("postponedAppointment")

        assertThat(field.isAnnotationPresent(ManyToOne::class.java))
            .`as`("postponedAppointment 필드에 @ManyToOne 부착")
            .isTrue()

        val joinColumn = field.getAnnotation(JoinColumn::class.java)
        assertThat(joinColumn)
            .`as`("postponedAppointment 필드에 @JoinColumn 부착")
            .isNotNull
        assertThat(joinColumn.name).isEqualTo("postponed_appointment_id")
    }

    @Test
    @DisplayName("AC2 — postponedAppointment 필드 타입은 Appointment")
    fun postponedAppointmentFieldType() {
        val field = Employee::class.java.getDeclaredField("postponedAppointment")
        assertThat(field.type).isEqualTo(Appointment::class.java)
    }
}
