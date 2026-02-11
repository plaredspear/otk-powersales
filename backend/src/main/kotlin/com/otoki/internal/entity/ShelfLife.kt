package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 유통기한 관리 Entity
 * 영업사원이 거래처별로 제품의 유통기한을 등록·관리하고,
 * 마감 전 푸시 알림을 받을 수 있다.
 */
@Entity
@Table(
    name = "shelf_lives",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_shelf_life_user_store_product",
            columnNames = ["user_id", "store_id", "product_id"]
        )
    ],
    indexes = [
        Index(name = "idx_shelf_life_user_expiry", columnList = "user_id, expiry_date"),
        Index(name = "idx_shelf_life_alert", columnList = "alert_date, alert_sent")
    ]
)
class ShelfLife(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    val store: Store,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    val product: Product,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Column(name = "store_name", nullable = false, length = 100)
    val storeName: String,

    @Column(name = "expiry_date", nullable = false)
    var expiryDate: LocalDate,

    @Column(name = "alert_date", nullable = false)
    var alertDate: LocalDate,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Column(name = "alert_sent", nullable = false)
    var alertSent: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 유통기한/알림일/설명을 수정한다.
     * alertDate가 변경되고 아직 발송 전이면 alertSent를 false로 리셋한다.
     */
    fun update(newExpiryDate: LocalDate, newAlertDate: LocalDate, newDescription: String?) {
        val alertDateChanged = this.alertDate != newAlertDate
        this.expiryDate = newExpiryDate
        this.alertDate = newAlertDate
        this.description = newDescription
        if (alertDateChanged && !this.alertSent) {
            this.alertSent = false
        }
        // alertDate가 변경되고 이미 발송된 경우 리셋
        if (alertDateChanged && this.alertSent) {
            this.alertSent = false
        }
    }

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
