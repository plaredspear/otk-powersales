package com.otoki.powersales.productexpiration.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.entity.Product
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.common.entity.AuditedEntity
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
    @Column(name = "account_name", length = 100)
    val accountName: String? = null,

    @Column(name = "account_id")
    val accountId: Int? = null,

    @HCColumn("account_code")
    @Column(name = "account_code", length = 100)
    val accountCode: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @HCColumn("employee_id")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @HCColumn("product_id")
    @Column(name = "product_name", length = 100)
    val productName: String? = null,

    @Column(name = "product_id")
    val productId: Long? = null,

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
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("updt_dt")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var account: Account? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var employee: Employee? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var product: Product? = null

    fun update(expirationDate: LocalDate, alarmDate: LocalDate, description: String?) {
        this.expirationDate = expirationDate
        this.alarmDate = alarmDate
        this.description = description
        this.updatedAt = LocalDateTime.now()
    }
}
