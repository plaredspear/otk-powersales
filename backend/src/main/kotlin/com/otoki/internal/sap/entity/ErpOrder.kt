package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "erp_order")
class ErpOrder(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sap_order_number", nullable = false, length = 20)
    val sapOrderNumber: String,

    @Column(name = "sap_account_code", length = 20)
    var sapAccountCode: String? = null,

    @Column(name = "sap_account_name", length = 100)
    var sapAccountName: String? = null,

    @Column(name = "delivery_request_date", length = 8)
    var deliveryRequestDate: String? = null,

    @Column(name = "order_date", length = 8)
    var orderDate: String? = null,

    @Column(name = "employee_code", length = 20)
    var employeeCode: String? = null,

    @Column(name = "employee_name", length = 50)
    var employeeName: String? = null,

    @Column(name = "order_sales_amount")
    var orderSalesAmount: Double? = null,

    @Column(name = "order_channel", length = 10)
    var orderChannel: String? = null,

    @Column(name = "order_channel_nm", length = 50)
    var orderChannelNm: String? = null,

    @Column(name = "order_type", length = 10)
    var orderType: String? = null,

    @Column(name = "order_type_nm", length = 50)
    var orderTypeNm: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
