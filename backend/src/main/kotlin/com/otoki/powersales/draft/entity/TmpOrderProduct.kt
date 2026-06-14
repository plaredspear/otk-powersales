package com.otoki.powersales.draft.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.math.BigDecimal
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity

/**
 * 임시저장 주문 라인 — Heroku 호환 + 신규 #596 컬럼 보강.
 *
 * Spec #596: `tmp_order_id` FK + 단위/단가/금액/라인번호 컬럼 추가 (V29 마이그레이션).
 * 레거시 `box_cnt`/`ea_cnt`/`total_cnt` 는 Heroku 호환을 위해 유지 (후속 스펙으로 drop).
 */
@Entity
@Table(name = "tmp_order_product")
@HerokuOnly("tmp_order_product")
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

    /** 헤더 FK — Spec #596 V29 추가. 정식 등록 후 cascade delete + 정규화. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tmp_order_id")
    var tmpOrder: TmpOrder? = null,

    /** 라인 번호 — Spec #596. */
    @Column(name = "line_number")
    var lineNumber: Int? = null,

    /** 단위 — `BOX` / `EA` (Spec #596). */
    @Column(name = "unit", length = 10)
    var unit: String? = null,

    /** 사용자 입력 수량 (단위 기준). */
    @Column(name = "quantity", precision = 16, scale = 2)
    var quantity: BigDecimal? = null,

    /** 환산 수량 — 낱개. */
    @Column(name = "quantity_pieces")
    var quantityPieces: Int? = null,

    /** 환산 수량 — 박스. */
    @Column(name = "quantity_boxes", precision = 16, scale = 2)
    var quantityBoxes: BigDecimal? = null,

    @Column(name = "unit_price", precision = 16, scale = 2)
    var unitPrice: BigDecimal? = null,

    @Column(name = "amount", precision = 16, scale = 2)
    var amount: BigDecimal? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()
