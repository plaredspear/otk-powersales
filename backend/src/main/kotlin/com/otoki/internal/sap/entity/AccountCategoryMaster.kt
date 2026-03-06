package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "account_category_master")
class AccountCategoryMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "account_code", nullable = false, unique = true, length = 20)
    val accountCode: String,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
