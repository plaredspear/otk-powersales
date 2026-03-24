package com.otoki.internal.productexpiration.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 유통기한 관리 Entity
 *
 * V1 테이블: salesforce2.product_expiration (원본: expirationdate__mng)
 */
@Entity
@Table(name = "product_expiration")
@HCTable("expirationdate__mng")
class ProductExpiration(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_expiration_id")
    val productExpirationId: Int = 0,

    @HCColumn("seq")
    @Column(name = "seq")
    val seq: Int = 0,

    @HCColumn("account_id")
    @Column(name = "account_id", length = 100)
    val accountId: String? = null,

    @HCColumn("account_code")
    @Column(name = "account_code", length = 100)
    val accountCode: String? = null,

    @HCColumn("employee_id")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @HCColumn("product_id")
    @Column(name = "product_id", length = 100)
    val productId: String? = null,

    @HCColumn("product_code")
    @Column(name = "product_code", length = 100)
    val productCode: String? = null,

    @Column(name = "expiration_date")
    @HCColumn("expiration_date")
    var expirationDate: LocalDate? = null,

    @HCColumn("alarm_date")
    @Column(name = "alarm_date")
    var alarmDate: LocalDate? = null,

    @HCColumn("description")
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @HCColumn("inst_dt")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("updt_dt")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity() {
    fun update(expirationDate: LocalDate, alarmDate: LocalDate, description: String?) {
        this.expirationDate = expirationDate
        this.alarmDate = alarmDate
        this.description = description
        this.updatedAt = LocalDateTime.now()
    }
}
