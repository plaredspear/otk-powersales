package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 현장 점검 테마 Entity
 * 현장 점검 시 선택할 수 있는 테마 정보를 관리한다.
 */
@Entity
@Table(
    name = "inspection_themes",
    indexes = [
        Index(
            name = "idx_theme_active_dates",
            columnList = "is_active, start_date, end_date"
        )
    ]
)
class InspectionTheme(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, length = 200)
    val name: String,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
