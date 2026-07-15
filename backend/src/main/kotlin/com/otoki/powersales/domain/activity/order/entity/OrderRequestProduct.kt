package com.otoki.powersales.domain.activity.order.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
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
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.domain.foundation.product.entity.Product
import jakarta.persistence.EntityListeners
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

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
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("주문요청상품")
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
class OrderRequestProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("주문요청상품ID")
    @Column(name = "order_request_product_id")
    val id: Long = 0,

    @SFField("DKRetail__RequestNumber__c")
    @Column(name = "order_request_sfid", length = 18)
    val orderRequestSfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("LineNumber__c")
    @FieldName("라인번호")
    // SF nillable=true (field-meta required=false) 정합 — 마이그레이션이 SF NULL row 를 보존하도록 nullable.
    @Column(name = "line_number")
    val lineNumber: BigDecimal? = null,

    @SFField("DKRetail__LineNumber__c")
    @FieldName("라인번호(DK)")
    @Column(name = "dk_line_number", length = 30)
    var dkLineNumber: String? = null,

    @SFField("ProductCode__c")
    @FieldName("상품코드")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "product_code", length = 255)
    val productCode: String? = null,

    @SFField("TotalQuantity_Box__c")
    @FieldName("총 박스환산치")
    // SF nillable=true 정합. 앱 신규 INSERT 는 기본값 0 유지.
    @Column(name = "quantity_boxes", precision = 18, scale = 2)
    val quantityBoxes: BigDecimal? = BigDecimal.ZERO,

    @SFField("TotalQuantity_Each__c")
    @FieldName("총주문낱개수량")
    @Column(name = "quantity_pieces")
    val quantityPieces: BigDecimal? = BigDecimal.ZERO,

    @SFField("DKRetail__OrderingUnit__c")
    @FieldName("발주단위")
    @Column(name = "unit", length = 40)
    val unit: String? = null,

    @FieldName("단가")
    @Column(name = "unit_price", precision = 16, scale = 2)
    val unitPrice: BigDecimal? = BigDecimal.ZERO,

    @SFField("DKRetail__TotalAmount__c")
    @FieldName("총금액")
    // SF nillable=true (field-meta required=false) 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "amount", precision = 18, scale = 2)
    val amount: BigDecimal? = BigDecimal.ZERO,

    @FieldName("박스당낱개수")
    @Column(name = "pieces_per_box")
    val piecesPerBox: Int? = 1,

    @FieldName("최소주문단위")
    @Column(name = "min_order_unit")
    val minOrderUnit: Int? = 1,

    @FieldName("공급수량")
    @Column(name = "supply_quantity")
    val supplyQuantity: Int? = 0,

    @FieldName("할인수량")
    @Column(name = "dc_quantity")
    val dcQuantity: Int? = 0,

    @SFField("DKRetail__LineChangeType__c")
    @FieldName("취소여부")
    @Column(name = "line_change_type", length = 10)
    var lineChangeType: String? = null,

    @FieldName("취소시각")
    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @FieldName("취소자")
    @Column(name = "cancelled_by", length = 8)
    var cancelledBy: String? = null,

    // -- Spec #858: 취소 요청 흔적 (SAP timeout 미확정 라인의 상세조회 정합 근거) --
    // SAP 취소 호출 전에 기록. cancelled_at/cancelled_by(확정) 와 대칭. @SFField 미부착 (SF 메타 미존재).

    @FieldName("취소요청시각")
    @Column(name = "cancel_requested_at")
    var cancelRequestedAt: LocalDateTime? = null,

    @FieldName("취소요청자")
    @Column(name = "cancel_requested_by", length = 8)
    var cancelRequestedBy: String? = null,

    @SFField("Status__c")
    @FieldName("상태")
    @Column(name = "status", length = 255)
    val status: String? = null,

    @SFField("DKRetail__ProductId__c")
    @Column(name = "product_sfid", length = 18)
    val productSfid: String? = null,

    @SFField("DKRetail__Box__c")
    @FieldName("Box")
    @Column(name = "box", precision = 18, scale = 0)
    val box: BigDecimal? = null,

    @SFField("DKRetail__Piece__c")
    @FieldName("낱개")
    @Column(name = "piece", precision = 18, scale = 0)
    val piece: BigDecimal? = null,

    @SFField("DKRetail__BoxQuantity__c")
    @FieldName("박스환산치")
    @Column(name = "box_quantity", precision = 18, scale = 3)
    val boxQuantity: BigDecimal? = null,

    @SFField("DKRetail__TotalCount__c")
    @FieldName("총수량")
    @Column(name = "dk_total_count", precision = 18, scale = 0)
    var dkTotalCount: BigDecimal? = null,

    @SFField("TotalCount__c")
    @FieldName("주문수량 (최소발주단위 기준)")
    @Column(name = "total_count", precision = 18, scale = 0)
    var totalCount: BigDecimal? = null,

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
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    // -- Spec #746 R-2 (DKRetail__ProductId__c FK 신설) --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null,
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
     * 취소 요청 흔적 기록 (Spec #858).
     *
     * SAP 취소 호출 **전에** `cancel_requested_at`/`cancel_requested_by` 를 기록해, timeout 등으로
     * SAP 응답을 확정받지 못해도 "이 라인에 취소를 시도했다"는 사실을 남긴다. `line_change_type` 은
     * 아직 미변경 — 확정은 SAP `'S'` 수신 또는 상세조회 정합 시에만 이뤄진다.
     */
    fun markCancelRequested(employeeId: String) {
        this.cancelRequestedAt = LocalDateTime.now()
        this.cancelRequestedBy = employeeId
    }

    /**
     * 상세조회 정합 대상 판정 (Spec #858).
     *
     * "취소를 요청했으나(cancel_requested_at 존재) 아직 확정되지 않은(line_change_type≠"X" && cancelled_at NULL)"
     * 라인인지 판정. 확정 라인은 두 조건(line_change_type/cancelled_at)을 AND 로 함께 봐 어느 한쪽만 세팅된
     * 비정상 상태에서도 재정합되지 않도록 멱등성을 이중 보장한다. SAP DefaultReason 존재 여부(SAP 취소 반영)는
     * 호출부에서 별도 판정한다.
     */
    fun isCancelReconcilable(): Boolean =
        cancelRequestedAt != null && !isCancelled() && cancelledAt == null

    /**
     * 취소 확정 정합 승격 (Spec #858).
     *
     * SAP timeout 으로 미확정 상태였던 라인을 상세조회 시 확정 취소로 승격한다. 취소자는 `cancel_requested_by`
     * (요청자)를 승계한다. [cancel] 과 동일하게 `line_change_type='X'` + `cancelled_at` 을 세팅한다.
     */
    fun reconcileCancel() {
        this.lineChangeType = LINE_CHANGE_TYPE_CANCEL
        this.cancelledAt = LocalDateTime.now()
        this.cancelledBy = cancelRequestedBy
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
