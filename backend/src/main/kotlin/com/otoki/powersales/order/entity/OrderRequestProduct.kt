package com.otoki.powersales.order.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문요청 라인 Entity (DKRetail__OrderRequestProduct__c).
 * 주문요청 1건에 포함된 개별 제품 라인.
 *
 * Spec #761:
 * - OwnerId polymorphic R-2 (`referenceTo = [Group, User]`) — owner_user_id / owner_group_id XOR.
 * - audit FK (createdBy / lastModifiedBy) 타입 Employee → User 전환.
 * - LineChangeType free-form string 보존 (`is_cancelled: Boolean` 우회 폐기) — SF 운영값 `{null, "X"}` 2-상태.
 *   cancel 도메인 메서드는 `LINE_CHANGE_TYPE_CANCEL` 상수로 set + `isCancelled()` 함수로 boolean 판정 제공.
 * - 타입·길이 정합 (§4) — unit/product_code/box_quantity/amount/quantity_boxes/line_number/quantity_pieces/dk_total_count/total_count.
 */
@Entity
@Table(
    name = "order_request_product",
    indexes = [
        Index(name = "idx_order_request_product_order_request_id", columnList = "order_request_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "idx_order_request_product_unique", columnNames = ["order_request_id", "line_number"]),
    ],
)
@SFObject("DKRetail__OrderRequestProduct__c")
@HCTable("dkretail__orderrequestproduct__c")
class OrderRequestProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_request_product_id")
    val id: Long = 0,

    @SFField("DKRetail__RequestNumber__c")
    @HCColumn("dkretail__requestnumber__c")
    @Column(name = "order_request_sfid", length = 18)
    val orderRequestSfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("LineNumber__c")
    @HCColumn("linenumber__c")
    @Column(name = "line_number", nullable = false)
    val lineNumber: Long,

    @SFField("DKRetail__LineNumber__c")
    @HCColumn("dkretail__linenumber__c")
    @Column(name = "dk_line_number", length = 30)
    var dkLineNumber: String? = null,

    @SFField("ProductCode__c")
    @HCColumn("productcode__c")
    @Column(name = "product_code", nullable = false, length = 255)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String,

    @SFField("TotalQuantity_Box__c")
    @HCColumn("totalquantity_box__c")
    @Column(name = "quantity_boxes", nullable = false, precision = 18, scale = 2)
    val quantityBoxes: BigDecimal = BigDecimal.ZERO,

    @SFField("TotalQuantity_Each__c")
    @HCColumn("totalquantity_each__c")
    @Column(name = "quantity_pieces", nullable = false)
    val quantityPieces: Long = 0,

    @SFField("DKRetail__OrderingUnit__c")
    @HCColumn("dkretail__orderingunit__c")
    @Column(name = "unit", nullable = false, length = 40)
    val unit: String,

    @Column(name = "unit_price", precision = 16, scale = 2)
    val unitPrice: BigDecimal? = BigDecimal.ZERO,

    @SFField("DKRetail__TotalAmount__c")
    @HCColumn("dkretail__totalamount__c")
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    val amount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "pieces_per_box")
    val piecesPerBox: Int? = 1,

    @Column(name = "min_order_unit")
    val minOrderUnit: Int? = 1,

    @Column(name = "supply_quantity")
    val supplyQuantity: Int? = 0,

    @Column(name = "dc_quantity")
    val dcQuantity: Int? = 0,

    @SFField("DKRetail__LineChangeType__c")
    @HCColumn("dkretail__linechangetype__c")
    @Column(name = "line_change_type", length = 10)
    var lineChangeType: String? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @Column(name = "cancelled_by", length = 8)
    var cancelledBy: String? = null,

    @SFField("Status__c")
    @Column(name = "status", length = 255)
    val status: String? = null,

    @SFField("DKRetail__ProductId__c")
    @Column(name = "product_sfid", length = 18)
    val productSfid: String? = null,

    @SFField("DKRetail__Box__c")
    @Column(name = "box", precision = 18, scale = 0)
    val box: BigDecimal? = null,

    @SFField("DKRetail__Piece__c")
    @Column(name = "piece", precision = 18, scale = 0)
    val piece: BigDecimal? = null,

    @SFField("DKRetail__BoxQuantity__c")
    @Column(name = "box_quantity", precision = 18, scale = 3)
    val boxQuantity: BigDecimal? = null,

    @SFField("DKRetail__TotalCount__c")
    @HCColumn("dkretail__totalcount__c")
    @Column(name = "dk_total_count", precision = 18, scale = 0)
    var dkTotalCount: BigDecimal? = null,

    @SFField("TotalCount__c")
    @HCColumn("totalcount__c")
    @Column(name = "total_count", precision = 18, scale = 0)
    var totalCount: BigDecimal? = null,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_request_id")
    val orderRequest: OrderRequest? = null,

    // -- Spec #761: OwnerId polymorphic R-2 (referenceTo = [Group, User]) --
    // owner_sfid 단일 컬럼이 SF 원본 식별자 보존. owner_user_id / owner_group_id 둘 중
    // 하나만 채워지며 XOR CHECK 제약으로 enforce. sfid prefix `005` = User / `00G` = Group.

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    // -- Spec #746 R-2 (DKRetail__ProductId__c FK 신설) --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: com.otoki.powersales.product.entity.Product? = null,
) : BaseEntity() {

    /**
     * 라인 취소 처리.
     *
     * SF 레거시 (`IF_REST_MOBILE_OrderCancelRequest.cls:92`) 패턴 — `lineChangeType` 을 취소 마커 `"X"` 로 set.
     * audit 컬럼 (`cancelled_at` / `cancelled_by`) 은 backend 신규 도입 (SF 메타 미존재).
     */
    fun cancel(employeeId: String) {
        this.lineChangeType = LINE_CHANGE_TYPE_CANCEL
        this.cancelledAt = LocalDateTime.now()
        this.cancelledBy = employeeId
    }

    /**
     * 라인 취소 여부 판정.
     *
     * SF 운영 도메인 `{null, "X"}` 2-상태 — `"X"` 마커이면 취소.
     */
    fun isCancelled(): Boolean = lineChangeType == LINE_CHANGE_TYPE_CANCEL

    companion object {
        /** SF 레거시 `DKRetail__LineChangeType__c` 취소 마커 (SAP outbound payload 호환). */
        const val LINE_CHANGE_TYPE_CANCEL = "X"
    }
}
