package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * 일정 Entity
 */
@Entity
@Table(name = "schedules")
class Schedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "store_name", nullable = false, length = 100)
    val storeName: String,

    @Column(name = "schedule_date", nullable = false)
    val scheduleDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(name = "type", nullable = false, length = 20)
    val type: String
)
