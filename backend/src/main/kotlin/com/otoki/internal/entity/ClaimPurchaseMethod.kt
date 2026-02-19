/*
package com.otoki.internal.entity

import jakarta.persistence.*

/ **
 * 구매 방법 Entity
 * 클레임 등록 시 제품을 구매한 방법을 관리한다.
 * 예: PM01-대형마트, PM02-편의점, PM03-온라인, PM04-슈퍼마켓, PM99-기타
 * /
@Entity
@Table(name = "claim_purchase_methods")
class ClaimPurchaseMethod(

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
*/
