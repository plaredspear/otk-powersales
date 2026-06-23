package com.otoki.powersales.domain.activity.order.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("ERP주문상품")
@Entity
@Table(name = "erp_order_product")
@SFObject("ERP_OrderProduct__c")
class ErpOrderProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("ERP주문상품ID")
    @Column(name = "erp_order_product_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erp_order_id")
    var erpOrder: ErpOrder? = null,

    @SFField("ERPOrderId__c")
    @Column(name = "erp_order_sfid", length = 18)
    var erpOrderSfid: String? = null,

    @SFField("SAPOrderNumber__c")
    @FieldName("ERP 주문 Key값(Back-End 용)")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "sap_order_number", length = 255)
    val sapOrderNumber: String? = null,

    @SFField("LineNumber__c")
    @FieldName("시퀀스")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "line_number", length = 255)
    val lineNumber: String? = null,

    @SFField("ExternalKey__c")
    @FieldName("ExternalKey")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "external_key", length = 255)
    val externalKey: String? = null,

    @SFField("ProductCode__c")
    @FieldName("제품코드")
    @Column(name = "product_code", length = 100)
    var productCode: String? = null,

    @SFField("ProductName__c")
    @FieldName("제품명")
    @Column(name = "product_name", length = 100)
    var productName: String? = null,

    @SFField("OrderQuantity__c")
    @FieldName("주문수량")
    @Column(name = "order_quantity")
    var orderQuantity: BigDecimal? = null,

    @SFField("Unit__c")
    @FieldName("주문단위")
    @Column(name = "unit", length = 10)
    var unit: String? = null,

    @SFField("ConfirmQuantity_Box__c")
    @FieldName("총납품수량(Box환산치)")
    @Column(name = "confirm_quantity_box", precision = 18, scale = 4)
    var confirmQuantityBox: BigDecimal? = null,

    @SFField("ConfirmQuantity__c")
    @FieldName("납품수량(최소주문단위)")
    @Column(name = "confirm_quantity", precision = 18, scale = 3)
    var confirmQuantity: BigDecimal? = null,

    @SFField("Confirm_Unit__c")
    @FieldName("납품단위(최소주문단위)")
    @Column(name = "confirm_unit", length = 255)
    var confirmUnit: String? = null,

    @SFField("DefaultReason__c")
    @FieldName("미납사유")
    @Column(name = "default_reason", length = 50)
    var defaultReason: String? = null,

    @SFField("LineItemStatus__c")
    @FieldName("Item처리상태")
    @Column(name = "line_item_status", length = 40)
    var lineItemStatus: String? = null,

    @SFField("OrderStatus__c")
    @FieldName("주문처리상태")
    @Column(name = "delivery_status", length = 30)
    var deliveryStatus: String? = null,

    @SFField("ShippingDriverName__c")
    @FieldName("배송기사이름")
    @Column(name = "shipping_driver_name", length = 30)
    var shippingDriverName: String? = null,

    @SFField("ShippingVehicle__c")
    @FieldName("배송차량번호")
    @Column(name = "shipping_vehicle", length = 30)
    var shippingVehicle: String? = null,

    @SFField("ShippingDriverPhone__c")
    @FieldName("배송기사연락처")
    @Column(name = "shipping_driver_phone", length = 40)
    var shippingDriverPhone: String? = null,

    @SFField("ShippingScheduleTime__c")
    @FieldName("배송예정시간")
    @Column(name = "shipping_schedule_time", length = 30)
    var shippingScheduleTime: String? = null,

    @SFField("ShippingCompleteTime__c")
    @FieldName("배송완료시간")
    @Column(name = "shipping_complete_time", length = 30)
    var shippingCompleteTime: String? = null,

    @SFField("ShippingQuantity_Box__c")
    @FieldName("배송수량(Box환산치)")
    @Column(name = "shipping_quantity_box", precision = 18, scale = 2)
    var shippingQuantityBox: BigDecimal? = null,

    @SFField("ShippingQuantity__c")
    @FieldName("배송수량(최소주문단위)")
    @Column(name = "shipping_quantity")
    var shippingQuantity: BigDecimal? = null,

    @SFField("OrderSalesLineAmount__c")
    @FieldName("납품금액")
    @Column(name = "order_sales_line_amount")
    var orderSalesLineAmount: BigDecimal? = null,

    @SFField("ShippingAmount__c")
    @FieldName("배송금액")
    @Column(name = "shipping_amount")
    var shippingAmount: BigDecimal? = null,

    @SFField("Plant__c")
    @FieldName("플랜트내역")
    @Column(name = "plant", length = 4)
    var plant: String? = null,

    @SFField("Plant_NM__c")
    @FieldName("플랜트내역 명")
    @Column(name = "plant_nm", length = 30)
    var plantNm: String? = null,

    @SFField("ReleaseQuantity__c")
    @FieldName("출고수량")
    @Column(name = "release_quantity")
    var releaseQuantity: BigDecimal? = null,

    @SFField("ReleaseAmount__c")
    @FieldName("출고금액")
    @Column(name = "release_amount")
    var releaseAmount: BigDecimal? = null,

    @SFField("BoxQuantity__c")
    @FieldName("박스환산치")
    @Column(name = "box_quantity", precision = 18, scale = 4)
    var boxQuantity: BigDecimal? = null,

    // -- sf-meta-diff Q1~Q3: OwnerId polymorphic R-2 + audit FK Employee → User --
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
) : BaseEntity()
