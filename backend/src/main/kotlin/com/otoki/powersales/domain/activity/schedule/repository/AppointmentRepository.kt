package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AppointmentRepository : JpaRepository<Appointment, Long> {

    /**
     * appointment.name 채번 — SF Appointment__c Name(AutoNumber `AP{00000000}`) 정합.
     *
     * name 은 SF AutoNumber 와 동일한 번호 공간(AP + 8자리)을 공유한다.
     * SF 데이터 sync 가 신규 시스템 시퀀스보다 큰 번호를 적재하면 nextval 만으로는 번호 충돌이 발생한다.
     * 이를 시점 의존 없이 해소하기 위해, 채번 때마다 nextval 과 "현재 데이터 최대 번호 + 1" 중 큰 값을
     * setval 로 확정한다 (PromotionRepository.getNextPromotionNumberSeq 와 동일 패턴).
     */
    @Query(
        value = """
            SELECT setval(
                'powersales.appointment_name_seq',
                GREATEST(
                    nextval('powersales.appointment_name_seq'),
                    COALESCE(
                        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
                           FROM powersales.appointment
                          WHERE name ~ '^AP[0-9]+$'),
                        0
                    ) + 1
                )
            )
        """,
        nativeQuery = true
    )
    fun getNextAppointmentNameSeq(): Long
}
