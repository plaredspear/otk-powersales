package com.otoki.powersales.draft.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.common.entity.AuditedEntity

/**
 * 임시저장 주문 헤더 — Heroku 호환 + Spec #596 보강.
 *
 * 사번당 1건 정책 (Q2). DB UNIQUE(`employee_id`) 제약은 V29 마이그레이션에서 추가.
 * 납기일 컬럼은 보유하지 않음 (Q8 — 레거시 정합).
 */
@Entity
@Table(
    name = "tmp_order",
    uniqueConstraints = [
        UniqueConstraint(name = "tmp_order_employee_id_uk", columnNames = ["employee_id"]),
    ],
)
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
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 헤더 삭제 시 라인도 cascade. orphanRemoval 로 컬렉션에서 제거된 라인은 자동 삭제.
     * Spec #596 — 정규화로 헤더-라인 1:N 매핑.
     */
    @OneToMany(
        mappedBy = "tmpOrder",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    var products: MutableList<TmpOrderProduct> = mutableListOf(),
) : AuditedEntity()
