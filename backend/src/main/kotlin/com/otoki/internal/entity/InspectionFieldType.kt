package com.otoki.internal.entity

import jakarta.persistence.*

/**
 * 현장 유형 Entity
 * 현장 점검 시 선택할 수 있는 유형 코드 정보를 관리한다.
 * 예: FT01-본매대, FT02-시식, FT03-행사매대, FT99-기타
 */
@Entity
@Table(name = "inspection_field_types")
class InspectionFieldType(

    @Id
    @Column(name = "code", nullable = false, length = 10)
    val code: String,

    @Column(name = "name", nullable = false, length = 50)
    val name: String,

    @Column(name = "sort_order", nullable = false)
    val sortOrder: Int = 0,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)
