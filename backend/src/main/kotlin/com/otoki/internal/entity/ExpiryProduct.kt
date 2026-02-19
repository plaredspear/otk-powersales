/*
package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate

/ **
 * 유통기한 관리 제품 Entity
 * /
@Entity
@Table(name = "expiry_products")
class ExpiryProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String,

    @Column(name = "store_name", nullable = false, length = 50)
    val storeName: String,

    @Column(name = "expiry_date", nullable = false)
    val expiryDate: LocalDate
)
*/
