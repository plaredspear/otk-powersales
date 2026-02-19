package com.otoki.internal.entity

import jakarta.persistence.*

@Entity
@Table(name = "safety_check_items")
class SafetyCheckItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // Phase2: SafetyCheckCategory 주석 처리로 관계 제거
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "category_id", nullable = false)
    // val category: SafetyCheckCategory,
    @Column(name = "category_id", nullable = false)
    val categoryId: Long = 0,

    @Column(name = "label", nullable = false, length = 100)
    val label: String,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int,

    @Column(name = "required", nullable = false)
    val required: Boolean = true,

    @Column(name = "active", nullable = false)
    val active: Boolean = true
)
