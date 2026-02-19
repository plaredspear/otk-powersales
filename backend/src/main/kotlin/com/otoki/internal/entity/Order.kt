/*
package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/ **
 * 주문 Entity
 * 영업사원이 거래처에 등록한 주문 정보를 관리한다.
 * /
@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_orders_user_id", columnList = "user_id"),
        Index(name = "idx_orders_store_id", columnList = "store_id"),
        Index(name = "idx_orders_order_date", columnList = "order_date"),
        Index(name = "idx_orders_delivery_date", columnList = "delivery_date"),
        Index(name = "idx_orders_approval_status", columnList = "approval_status")
    ]
)
class Order(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "order_request_number", nullable = false, unique = true, length = 20)
    val orderRequestNumber: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    val store: Store,

    @Column(name = "order_date", nullable = false)
    val orderDate: LocalDate,

    @Column(name = "delivery_date", nullable = false)
    val deliveryDate: LocalDate,

    @Column(name = "total_amount", nullable = false)
    val totalAmount: Long = 0,

    @Column(name = "total_approved_amount", nullable = false)
    val totalApprovedAmount: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    var approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,

    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = false,

    @Column(name = "client_deadline_time", length = 5)
    val clientDeadlineTime: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    val items: MutableList<OrderItem> = mutableListOf()

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    val processingRecords: MutableList<OrderProcessingRecord> = mutableListOf()

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    val rejections: MutableList<OrderRejection> = mutableListOf()

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
*/
