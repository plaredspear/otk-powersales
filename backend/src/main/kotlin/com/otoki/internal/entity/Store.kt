package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 거래처 마스터 Entity
 * 거래처의 기본 정보(대표자명, 전화번호 등)를 관리한다.
 * StoreSchedule의 storeId를 통해 연결된다.
 */
@Entity
@Table(name = "stores")
class Store(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "store_code", nullable = false, unique = true, length = 20)
    val storeCode: String,

    @Column(name = "store_name", nullable = false, length = 100)
    val storeName: String,

    @Column(name = "address", length = 200)
    val address: String? = null,

    @Column(name = "representative_name", length = 50)
    val representativeName: String? = null,

    @Column(name = "phone_number", length = 20)
    val phoneNumber: String? = null,

    @Column(name = "credit_limit", nullable = false)
    val creditLimit: Long = 0,

    @Column(name = "used_credit", nullable = false)
    val usedCredit: Long = 0,

    @Column(name = "credit_updated_at")
    val creditUpdatedAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
