package com.otoki.powersales.account.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*

@Entity
@Table(name = "account_category_master")
@SFObject("AccountCategoryMaster__c")
class AccountCategoryMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_category_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("AccountCode__c")
    @Column(name = "account_code", nullable = false, unique = true, length = 20)
    val accountCode: String,

    @SFField("Name")
    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @SFField("useSearch__c")
    @Column(name = "use_search", nullable = false)
    var useSearch: Boolean = false
) : BaseEntity()
