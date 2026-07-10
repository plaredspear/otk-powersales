package com.otoki.powersales.domain.activity.draft.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 임시저장 주문 헤더 — Heroku 호환 + Spec #596 보강.
 *
 * 사번당 1건 정책 (Q2). DB UNIQUE(`employee_id`) 제약은 V29 마이그레이션에서 추가.
 * 납기일은 `order_date`(`tmpOrderDate`) 에 보관 — 레거시 Heroku `saveTemp` 가 화면
 * 납기일(`#DeliveryRequestDate`)을 `tmp_orderdate` 컬럼에 저장·복원했던 것과 정합.
 */
@DomainName("임시저장 주문")
@Entity
@Table(
    name = "tmp_order",
    uniqueConstraints = [
        UniqueConstraint(name = "tmp_order_employee_id_uk", columnNames = ["employee_id"]),
    ],
)
@HerokuOnly("tmp_order")
class TmpOrder(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("임시저장주문ID")
    @Column(name = "tmp_order_id")
    val id: Long = 0,

    @HCColumn("tmp_employeecode")
    @FieldName("사번")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_accountcode")
    @FieldName("거래처유형코드")
    @Column(name = "account_code", length = 80)
    var tmpAccountCode: String? = null,

    // 컬럼명은 tmp_orderdate(주문일자)지만 레거시 정합상 실제 담기는 값은 납기일(DeliveryRequestDate).
    @HCColumn("tmp_orderdate")
    @FieldName("납기일")
    @Column(name = "order_date")
    var tmpOrderDate: LocalDate? = null,

    @HCColumn("tmp_totalamount")
    @FieldName("총주문금액 (원)")
    @Column(name = "total_amount", length = 80)
    var tmpTotalAmount: String? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    var accountId: Long? = null,

    @FieldName("사원ID")
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
