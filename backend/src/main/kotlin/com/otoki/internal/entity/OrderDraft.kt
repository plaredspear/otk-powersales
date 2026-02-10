package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 임시저장 주문서 Entity
 * 사용자당 최근 1건만 유지
 */
@Entity
@Table(
    name = "order_drafts",
    indexes = [
        Index(name = "idx_order_drafts_user_id", columnList = "user_id", unique = true)
    ]
)
class OrderDraft(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    val store: Store,

    @Column(name = "delivery_date", nullable = false)
    val deliveryDate: LocalDate,

    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @OneToMany(mappedBy = "orderDraft", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val items: MutableList<OrderDraftItem> = mutableListOf()

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
