package com.otoki.internal.entity

import jakarta.persistence.*

/**
 * 월매출 Entity
 * 외부 시스템(Orora 영업, SAP)에서 동기화된 월별 매출 집계 데이터
 * TODO: 외부 시스템 연동 구현 후 실제 데이터로 대체
 */
@Entity
@Table(
    name = "monthly_sales",
    indexes = [
        Index(name = "idx_monthly_sales_customer_id", columnList = "customer_id"),
        Index(name = "idx_monthly_sales_year_month", columnList = "year_month"),
        Index(name = "idx_monthly_sales_category", columnList = "category")
    ]
)
class MonthlySales(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "customer_id", nullable = false, length = 50)
    val customerId: String,

    @Column(name = "year_month", nullable = false, length = 6)
    val yearMonth: String,

    @Column(name = "category", nullable = false, length = 50)
    val category: String,

    @Column(name = "target_amount", nullable = false)
    val targetAmount: Long = 0,

    @Column(name = "achieved_amount", nullable = false)
    val achievedAmount: Long = 0
)
