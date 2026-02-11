package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 클레임 Entity
 * 사용자가 등록한 클레임 정보를 관리한다.
 */
@Entity
@Table(
    name = "claims",
    indexes = [
        Index(name = "idx_claim_user_created", columnList = "user_id,created_at"),
        Index(name = "idx_claim_store", columnList = "store_id")
    ]
)
class Claim(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    val store: Store,

    @Column(name = "store_name", nullable = false, length = 100)
    val storeName: String,

    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "date_type", nullable = false, length = 20)
    val dateType: ClaimDateType,

    @Column(name = "date", nullable = false)
    val date: LocalDate,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ClaimCategory,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    val subcategory: ClaimSubcategory,

    @Column(name = "defect_description", nullable = false, length = 1000)
    val defectDescription: String,

    @Column(name = "defect_quantity", nullable = false)
    val defectQuantity: Int,

    @Column(name = "purchase_amount")
    val purchaseAmount: Int? = null,

    @Column(name = "purchase_method_code", length = 10)
    val purchaseMethodCode: String? = null,

    @Column(name = "purchase_method_name", length = 50)
    val purchaseMethodName: String? = null,

    @Column(name = "request_type_code", length = 10)
    val requestTypeCode: String? = null,

    @Column(name = "request_type_name", length = 50)
    val requestTypeName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: ClaimStatus = ClaimStatus.SUBMITTED,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
