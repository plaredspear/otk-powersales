package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "daily_sales_history")
class DailySalesHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sap_account_code", nullable = false, length = 20)
    val sapAccountCode: String,

    @Column(name = "sales_date", nullable = false, length = 8)
    val salesDate: String,

    @Column(name = "external_key", nullable = false, unique = true, length = 30)
    val externalKey: String,

    @Column(name = "erp_sales_amount1")
    var erpSalesAmount1: Double? = null,

    @Column(name = "erp_sales_amount2")
    var erpSalesAmount2: Double? = null,

    @Column(name = "erp_sales_amount3")
    var erpSalesAmount3: Double? = null,

    @Column(name = "erp_distribution_amount1")
    var erpDistributionAmount1: Double? = null,

    @Column(name = "erp_distribution_amount2")
    var erpDistributionAmount2: Double? = null,

    @Column(name = "erp_distribution_amount3")
    var erpDistributionAmount3: Double? = null,

    @Column(name = "ledger_amount")
    var ledgerAmount: Double? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
