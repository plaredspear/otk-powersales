package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "erp_order_product")
class ErpOrderProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "erp_order_id", nullable = false)
    var erpOrder: ErpOrder,

    @Column(name = "sap_order_number", nullable = false, length = 20)
    val sapOrderNumber: String,

    @Column(name = "line_number", nullable = false, length = 10)
    val lineNumber: String,

    @Column(name = "external_key", nullable = false, length = 50)
    val externalKey: String,

    @Column(name = "product_code", length = 20)
    var productCode: String? = null,

    @Column(name = "product_name", length = 100)
    var productName: String? = null,

    @Column(name = "order_quantity")
    var orderQuantity: Double? = null,

    @Column(name = "unit", length = 10)
    var unit: String? = null,

    @Column(name = "confirm_quantity_box")
    var confirmQuantityBox: Double? = null,

    @Column(name = "confirm_quantity")
    var confirmQuantity: Double? = null,

    @Column(name = "confirm_unit", length = 10)
    var confirmUnit: String? = null,

    @Column(name = "default_reason", length = 100)
    var defaultReason: String? = null,

    @Column(name = "line_item_status", length = 20)
    var lineItemStatus: String? = null,

    @Column(name = "delivery_status", length = 10)
    var deliveryStatus: String? = null,

    @Column(name = "shipping_driver_name", length = 50)
    var shippingDriverName: String? = null,

    @Column(name = "shipping_vehicle", length = 20)
    var shippingVehicle: String? = null,

    @Column(name = "shipping_driver_phone", length = 20)
    var shippingDriverPhone: String? = null,

    @Column(name = "shipping_schedule_time", length = 20)
    var shippingScheduleTime: String? = null,

    @Column(name = "shipping_complete_time", length = 20)
    var shippingCompleteTime: String? = null,

    @Column(name = "shipping_quantity_box")
    var shippingQuantityBox: Double? = null,

    @Column(name = "shipping_quantity")
    var shippingQuantity: Double? = null,

    @Column(name = "order_sales_line_amount")
    var orderSalesLineAmount: Double? = null,

    @Column(name = "shipping_amount")
    var shippingAmount: Double? = null,

    @Column(name = "plant", length = 10)
    var plant: String? = null,

    @Column(name = "plant_nm", length = 50)
    var plantNm: String? = null,

    @Column(name = "release_quantity")
    var releaseQuantity: Double? = null,

    @Column(name = "release_amount")
    var releaseAmount: Double? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
