package com.otoki.powersales.account.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
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
    var useSearch: Boolean = false,

    // -- Spec #704: Reference R-2 (OwnerId) — sfid buffer + Employee FK --
    // owner_sfid: Heroku Connect sync 가 채우는 buffer (SF User Id).
    // owner: SalesforceMigrationTool 이 SF User → Employee 매핑으로 채우는 FK.

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    // -- Spec #704: Group A (CreatedById / LastModifiedById) sfid + Employee FK --

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null
) : BaseEntity()
