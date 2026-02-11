package com.otoki.internal.entity

import jakarta.persistence.*

/**
 * 요청사항 Entity
 * 클레임 등록 시 사용자가 선택하는 요청사항을 관리한다.
 * 예: RT01-교환, RT02-환불, RT03-원인 규명, RT99-기타
 */
@Entity
@Table(name = "claim_request_types")
class ClaimRequestType(

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
