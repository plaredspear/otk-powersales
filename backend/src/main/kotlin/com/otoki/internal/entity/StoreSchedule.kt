package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate

/**
 * 거래처 일정 Entity (진열마스터 확정 스케줄)
 */
@Entity
@Table(name = "store_schedules")
class StoreSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Column(name = "store_name", nullable = false, length = 100)
    val storeName: String,

    @Column(name = "store_code", nullable = false, length = 20)
    val storeCode: String,

    @Column(name = "work_category", nullable = false, length = 20)
    val workCategory: String,

    @Column(name = "address", length = 200)
    val address: String? = null,

    @Column(name = "schedule_date", nullable = false)
    val scheduleDate: LocalDate
)
