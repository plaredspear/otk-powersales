package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 현장 점검 Entity
 * 영업사원이 거래처에서 수행한 현장 점검 정보를 관리한다.
 * 자사 점검과 경쟁사 점검을 분류(category)에 따라 구분한다.
 */
@Entity
@Table(
    name = "inspections",
    indexes = [
        Index(
            name = "idx_inspection_user_date",
            columnList = "user_id, inspection_date"
        ),
        Index(
            name = "idx_inspection_user_date_category",
            columnList = "user_id, inspection_date, category"
        )
    ]
)
class Inspection(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    val store: Store,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    val theme: InspectionTheme,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    val category: InspectionCategory,

    @Column(name = "store_name", nullable = false, length = 200)
    val storeName: String,

    @Column(name = "inspection_date", nullable = false)
    val inspectionDate: LocalDate,

    @Column(name = "field_type_code", nullable = false, length = 10)
    val fieldTypeCode: String,

    @Column(name = "field_type_name", nullable = false, length = 50)
    val fieldTypeName: String,

    // 자사 점검 관련 필드
    @Column(name = "description", length = 500)
    val description: String? = null,

    @Column(name = "product_code", length = 20)
    val productCode: String? = null,

    @Column(name = "product_name", length = 200)
    val productName: String? = null,

    // 경쟁사 점검 관련 필드
    @Column(name = "competitor_name", length = 100)
    val competitorName: String? = null,

    @Column(name = "competitor_activity", length = 500)
    val competitorActivity: String? = null,

    @Column(name = "competitor_tasting")
    val competitorTasting: Boolean? = null,

    @Column(name = "competitor_product_name", length = 100)
    val competitorProductName: String? = null,

    @Column(name = "competitor_product_price")
    val competitorProductPrice: Int? = null,

    @Column(name = "competitor_sales_quantity")
    val competitorSalesQuantity: Int? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
// Phase2: InspectionPhoto 주석 처리로 관계 제거
// {
//     @OneToMany(mappedBy = "inspection", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
//     val photos: MutableList<InspectionPhoto> = mutableListOf()
// }
