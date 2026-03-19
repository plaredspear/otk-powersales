package com.otoki.internal.sap.entity

import com.otoki.internal.common.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "account_category_master")
class AccountCategoryMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "account_code", nullable = false, unique = true, length = 20)
    val accountCode: String,

    @Column(name = "name", nullable = false, length = 100)
    var name: String
) : BaseEntity()
