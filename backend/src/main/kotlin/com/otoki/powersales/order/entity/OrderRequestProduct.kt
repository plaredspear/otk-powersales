package com.otoki.powersales.order.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
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
    val lineNumber: Int,

    @SFField("DKRetail__LineNumber__c")
    @HCColumn("dkretail__linenumber__c")
    @Column(name = "dk_line_number", length = 30)
    var dkLineNumber: String? = null,

    @SFField("ProductCode__c")
    @HCColumn("productcode__c")
    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String,

    @SFField("TotalQuantity_Box__c")
    @HCColumn("totalquantity_box__c")
    @Column(name = "quantity_boxes", nullable = false, precision = 16, scale = 2)
    val quantityBoxes: BigDecimal = BigDecimal.ZERO,

    @SFField("TotalQuantity_Each__c")
    @HCColumn("totalquantity_each__c")
    @Column(name = "quantity_pieces", nullable = false)
    val quantityPieces: Int = 0,

    @SFField("DKRetail__OrderingUnit__c")
    @HCColumn("dkretail__orderingunit__c")
    @Column(name = "unit", nullable = false, length = 10)
    val unit: String,

    @Column(name = "unit_price", nullable = false, precision = 16, scale = 2)
    val unitPrice: BigDecimal = BigDecimal.ZERO,

    @SFField("DKRetail__TotalAmount__c")
    @HCColumn("dkretail__totalamount__c")
    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    val amount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "pieces_per_box", nullable = false)
    val piecesPerBox: Int = 1,

    @Column(name = "min_order_unit", nullable = false)
    val minOrderUnit: Int = 1,

    @Column(name = "supply_quantity", nullable = false)
    val supplyQuantity: Int = 0,

    @Column(name = "dc_quantity", nullable = false)
    val dcQuantity: Int = 0,

    @SFField("DKRetail__LineChangeType__c")
    @HCColumn("dkretail__linechangetype__c")
    @Column(name = "is_cancelled", nullable = false)
    var isCancelled: Boolean = false,

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
    @Column(name = "box_quantity", precision = 15, scale = 3)
    val boxQuantity: BigDecimal? = null,

    @SFField("DKRetail__TotalCount__c")
    @HCColumn("dkretail__totalcount__c")
    @Column(name = "dk_total_count")
    var dkTotalCount: Double? = null,

    @SFField("TotalCount__c")
    @HCColumn("totalcount__c")
    @Column(name = "total_count")
    var totalCount: Double? = null,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_request_id", nullable = false)
    val orderRequest: OrderRequest,

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
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,
) : BaseEntity() {

    fun cancel(employeeId: String) {
        this.isCancelled = true
        this.cancelledAt = LocalDateTime.now()
        this.cancelledBy = employeeId
    }
}
