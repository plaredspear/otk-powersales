package com.otoki.powersales.domain.activity.productexpiration.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.product.entity.Product
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName
/**
 * 유통기한 관리 Entity
 *
 * V1 테이블: powersales.product_expiration (원본: expirationdate__mng)
 */
@DomainName("유통기한관리")
@Entity
@Table(name = "product_expiration")
@HerokuOnly("expirationdate__mng")
class ProductExpiration(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("유통기한관리ID")
    @Column(name = "product_expiration_id")
    val productExpirationId: Int = 0,

    @HCColumn("seq")
    @FieldName("순번")
    @Column(name = "seq")
    val seq: Int = 0,

    @HCColumn("account_id")
    @FieldName("거래처명")
    @Column(name = "account_name", length = 100)
    val accountName: String? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    val accountId: Long? = null,

    @HCColumn("account_code")
    @FieldName("거래처유형코드")
    @Column(name = "account_code", length = 100)
    val accountCode: String? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @HCColumn("employee_id")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @HCColumn("product_id")
    @FieldName("제품명")
    @Column(name = "product_name", length = 100)
    val productName: String? = null,

    @FieldName("제품ID")
    @Column(name = "product_id")
    val productId: Long? = null,

    @HCColumn("product_code")
    @FieldName("제품코드")
    @Column(name = "product_code", length = 100)
    val productCode: String? = null,

    @FieldName("유통기한")
    @Column(name = "expiration_date")
    @HCColumn("expiration_date")
    var expirationDate: LocalDate? = null,

    @HCColumn("alarm_date")
    @FieldName("알람일자")
    @Column(name = "alarm_date")
    var alarmDate: LocalDate? = null,

    @HCColumn("description")
    @FieldName("행사대체제품")
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
    }
}
