package com.otoki.powersales.order.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*

@Entity
@Table(name = "erp_order_product")
@SFObject("ERP_OrderProduct__c")
class ErpOrderProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "erp_order_product_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erp_order_id", nullable = false)
    var erpOrder: ErpOrder,

    @Column(name = "sap_order_number", nullable = false, length = 20)
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
    var boxQuantity: java.math.BigDecimal? = null
) : BaseEntity()
