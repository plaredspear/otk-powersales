package com.otoki.internal.entity

import jakarta.persistence.*

/**
 * 클레임 종류2 Entity
 * 클레임의 세부 카테고리를 관리한다. 종류1에 속한다.
 * 예: 벌레, 금속, 비닐, 곰팡이, 변색 등
 */
@Entity
@Table(name = "claim_subcategories")
class ClaimSubcategory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ClaimCategory,

    @Column(name = "name", nullable = false, length = 50)
    val name: String,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)
