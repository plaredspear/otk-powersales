package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 일매출 Entity
 * 영업사원이 매일 등록하는 행사 매출 정보
 */
@Entity
@Table(
    name = "daily_sales",
    indexes = [
        Index(name = "idx_daily_sales_event_id", columnList = "event_id"),
        Index(name = "idx_daily_sales_employee_id", columnList = "employee_id"),
        Index(name = "idx_daily_sales_sales_date", columnList = "sales_date"),
        Index(name = "idx_daily_sales_status", columnList = "status")
    ]
)
class DailySales(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "event_id", nullable = false, length = 50)
    val eventId: String,

    @Column(name = "employee_id", nullable = false, length = 20)
    val employeeId: String,

    @Column(name = "sales_date", nullable = false)
    val salesDate: LocalDate,

    // 대표제품 정보
    @Column(name = "main_product_price")
    var mainProductPrice: Int? = null,

    @Column(name = "main_product_quantity")
    var mainProductQuantity: Int? = null,

    @Column(name = "main_product_amount")
    var mainProductAmount: Int? = null,

    // 기타제품 정보
    @Column(name = "sub_product_code", length = 20)
    var subProductCode: String? = null,

    @Column(name = "sub_product_quantity")
    var subProductQuantity: Int? = null,

    @Column(name = "sub_product_amount")
    var subProductAmount: Int? = null,

    // 사진 URL
    @Column(name = "photo_url", length = 500)
    var photoUrl: String? = null,

    // 상태: DRAFT(임시저장), REGISTERED(등록완료)
    @Column(name = "status", nullable = false, length = 20)
    var status: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }

    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_REGISTERED = "REGISTERED"
    }
}
