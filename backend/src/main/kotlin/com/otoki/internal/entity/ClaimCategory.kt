package com.otoki.internal.entity

import jakarta.persistence.*

/**
 * 클레임 종류1 Entity
 * 클레임의 대분류 카테고리를 관리한다.
 * 예: 이물, 변질/변패, 포장불량, 유통기한, 기타
 */
@Entity
@Table(name = "claim_categories")
class ClaimCategory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "name", nullable = false, unique = true, length = 50)
    val name: String,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)
