package com.otoki.powersales.order.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*

@Entity
@Table(name = "erp_order_product")
@SFObject("ERP_OrderProduct__c")
@HCTable("erp_orderproduct__c")
class ErpOrderProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "erp_order_product_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erp_order_id", nullable = false)
    var erpOrder: ErpOrder,

    @SFField("ERPOrderId__c")
    @HCColumn("erporderid__c")
    @Column(name = "erp_order_sfid", length = 18)
    var erpOrderSfid: String? = null,

    @SFField("SAPOrderNumber__c")
    @HCColumn("sapordernumber__c")
    @Column(name = "sap_order_number", nullable = false, length = 255)
    val sapOrderNumber: String,

    @SFField("LineNumber__c")
    @Column(name = "line_number", nullable = false, length = 10)
    val lineNumber: String,

    @SFField("ExternalKey__c")
    @Column(name = "external_key", nullable = false, length = 50)
    val externalKey: String,

    @SFField("ProductCode__c")
    @Column(name = "product_code", length = 20)
    var productCode: String? = null,

    @SFField("ProductName__c")
    @Column(name = "product_name", length = 100)
    var productName: String? = null,

    @SFField("OrderQuantity__c")
    @Column(name = "order_quantity")
    var orderQuantity: Double? = null,

    @SFField("Unit__c")
    @Column(name = "unit", length = 10)
    var unit: String? = null,

    @SFField("ConfirmQuantity_Box__c")
    @Column(name = "confirm_quantity_box")
    var confirmQuantityBox: Double? = null,

    @SFField("ConfirmQuantity__c")
    @Column(name = "confirm_quantity")
    var confirmQuantity: Double? = null,

    @SFField("Confirm_Unit__c")
    @Column(name = "confirm_unit", length = 10)
    var confirmUnit: String? = null,

    @SFField("DefaultReason__c")
    @Column(name = "default_reason", length = 100)
    var defaultReason: String? = null,

    @SFField("LineItemStatus__c")
    @Column(name = "line_item_status", length = 20)
    var lineItemStatus: String? = null,

    @SFField("OrderStatus__c")
    @Column(name = "delivery_status", length = 10)
    var deliveryStatus: String? = null,

    @SFField("ShippingDriverName__c")
    @Column(name = "shipping_driver_name", length = 50)
    var shippingDriverName: String? = null,

    @SFField("ShippingVehicle__c")
    @Column(name = "shipping_vehicle", length = 20)
    var shippingVehicle: String? = null,

    @SFField("ShippingDriverPhone__c")
    @Column(name = "shipping_driver_phone", length = 20)
    var shippingDriverPhone: String? = null,

    @SFField("ShippingScheduleTime__c")
    @Column(name = "shipping_schedule_time", length = 20)
    var shippingScheduleTime: String? = null,

    @SFField("ShippingCompleteTime__c")
    @Column(name = "shipping_complete_time", length = 20)
    var shippingCompleteTime: String? = null,

    @SFField("ShippingQuantity_Box__c")
    @Column(name = "shipping_quantity_box")
    var shippingQuantityBox: Double? = null,

    @SFField("ShippingQuantity__c")
    @Column(name = "shipping_quantity")
    var shippingQuantity: Double? = null,

    @SFField("OrderSalesLineAmount__c")
    @Column(name = "order_sales_line_amount")
    var orderSalesLineAmount: Double? = null,

    @SFField("ShippingAmount__c")
    @Column(name = "shipping_amount")
    var shippingAmount: Double? = null,

    @SFField("Plant__c")
    @Column(name = "plant", length = 10)
    var plant: String? = null,

    @SFField("Plant_NM__c")
    @Column(name = "plant_nm", length = 50)
    var plantNm: String? = null,

    @SFField("ReleaseQuantity__c")
    @Column(name = "release_quantity")
    var releaseQuantity: Double? = null,

    @SFField("ReleaseAmount__c")
    @Column(name = "release_amount")
    var releaseAmount: Double? = null,

    // -- Spec #611: SF 누락 컬럼 신규 도입 (Q1 옵션 1) --

    @SFField("BoxQuantity__c")
    @Column(name = "box_quantity", precision = 18, scale = 0)
    var boxQuantity: java.math.BigDecimal? = null,

    // -- Spec #747 카테고리 A — D 분류 누락 --
    @SFField("OrderDate__c")
    @Column(name = "order_date")
    var orderDate: java.time.LocalDate? = null,

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
) : BaseEntity()
