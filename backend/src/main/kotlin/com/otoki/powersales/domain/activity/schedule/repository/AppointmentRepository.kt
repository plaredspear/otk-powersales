package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AppointmentRepository : JpaRepository<Appointment, Long> {

    /**
     * appointment.name 채번 — SF Appointment__c Name(AutoNumber `AP{00000000}`) 정합.
     * 시퀀스 nextval 단독.
     *
     * MAX 보정은 [syncNameSeq] 가 담당하며, 번호가 외부에서 주입될 수 있는 시점
     * (부팅 1회 / SF 마이그레이션 직후)에만 실행한다 — `NameSequenceSyncService`.
     * SAP 인사발령 인바운드가 행마다 본 채번을 호출하므로 hot path 다.
     */
    @Query(
        value = "SELECT nextval('powersales.appointment_name_seq')",
        nativeQuery = true
    )
    fun getNextAppointmentNameSeq(): Long

    /**
     * name 시퀀스를 기존 데이터 최대 번호 위로 끌어올린다 (멱등).
     * MAX 대상 표현식에는 부분 인덱스(`idx_appointment_name_seq_num`)가 있다.
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
    fun syncNameSeq(): Long
}
