package com.otoki.powersales.draft.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "tmp_order_product")
@HCTable("tmp_order_product")
class TmpOrderProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tmp_order_product_id")
    val id: Long = 0,

    @HCColumn("tmp_employeecode")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_productcode")
    @Column(name = "product_code", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_boxcnt")
    @Column(name = "box_cnt", length = 80)
    var tmpBoxCnt: String? = null,

    @HCColumn("tmp_eacnt")
    @Column(name = "ea_cnt", length = 80)
    var tmpEaCnt: String? = null,

    @HCColumn("tmp_totalcnt")
    @Column(name = "total_cnt", length = 80)
    var tmpTotalCnt: String? = null,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @Column(name = "product_id")
    var productId: Long? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
