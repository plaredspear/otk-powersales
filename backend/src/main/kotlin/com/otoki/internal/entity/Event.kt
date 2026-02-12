package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 행사 Entity
 * 영업사원이 담당하는 제품 판촉 행사 정보
 */
@Entity
@Table(
    name = "events",
    indexes = [
        Index(name = "idx_events_event_id", columnList = "event_id", unique = true),
        Index(name = "idx_events_assignee_id", columnList = "assignee_id"),
        Index(name = "idx_events_customer_id", columnList = "customer_id"),
        Index(name = "idx_events_start_date", columnList = "start_date"),
        Index(name = "idx_events_end_date", columnList = "end_date")
    ]
)
class Event(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "event_id", nullable = false, unique = true, length = 50)
    val eventId: String,

    @Column(name = "event_type", nullable = false, length = 50)
    val eventType: String,

    @Column(name = "event_name", nullable = false, length = 200)
    val eventName: String,

    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,

    @Column(name = "customer_id", nullable = false, length = 50)
    val customerId: String,

    @Column(name = "assignee_id", nullable = false, length = 20)
    val assigneeId: String,

    @Column(name = "target_amount", nullable = false)
    val targetAmount: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
