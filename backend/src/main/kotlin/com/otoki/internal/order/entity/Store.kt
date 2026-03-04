/* Order 모듈 전체 비활성화 — DB 테이블 미존재
package com.otoki.internal.order.entity

import jakarta.persistence.*

/**
 * 거래처 Entity (주문 모듈용)
 * Order 엔티티가 참조하는 거래처 정보.
 */
@Entity
@Table(name = "stores")
class Store(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "store_name", nullable = false, length = 100)
    val storeName: String = "",

    @Column(name = "store_code", length = 20)
    val storeCode: String? = null
)
*/