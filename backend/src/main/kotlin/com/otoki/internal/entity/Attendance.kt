package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 출근등록 Entity
 */
@Entity
@Table(
    name = "attendances",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_attendance_user_store_date",
            columnNames = ["user_id", "store_id", "attendance_date"]
        )
    ]
)
class Attendance(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false, length = 20)
    val workType: AttendanceWorkType,

    @Column(name = "attendance_date", nullable = false)
    val attendanceDate: LocalDate,

    @Column(name = "registered_at", nullable = false)
    val registeredAt: LocalDateTime = LocalDateTime.now()
)
