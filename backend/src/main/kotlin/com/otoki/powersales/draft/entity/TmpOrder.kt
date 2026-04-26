package com.otoki.powersales.draft.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.common.entity.AuditedEntity
@Entity
@Table(name = "tmp_order")
@HCTable("tmp_order")
class TmpOrder(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tmp_order_id")
    val id: Long = 0,

    @HCColumn("tmp_employeecode")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_accountcode")
    @Column(name = "account_code", length = 80)
    var tmpAccountCode: String? = null,

    @HCColumn("tmp_orderdate")
    @Column(name = "order_date")
    var tmpOrderDate: LocalDate? = null,

    @HCColumn("tmp_totalamount")
    @Column(name = "total_amount", length = 80)
    var tmpTotalAmount: String? = null,

    @Column(name = "account_id")
    var accountId: Long? = null,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()