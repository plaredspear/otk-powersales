package com.otoki.powersales.inspection.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 현장 점검 테마 Entity
 * V1 스키마: theme__c
 */
@Entity
@Table(name = "inspection_theme")
@SFObject("Theme__c")
@HCTable("theme__c")
class InspectionTheme(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_theme_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("Title__c")
    @HCColumn("title__c")
    @Column(name = "title", length = 250)
    val title: String? = null,

    @SFField("StartDate__c")
    @HCColumn("startdate__c")
    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @HCColumn("enddate__c")
    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @SFField("Department__c")
    @HCColumn("department__c")
    @Column(name = "department", length = 100)
    val department: String? = null,

    @SFField("BranchCode__c")
    @HCColumn("branchcode__c")
    @Column(name = "branch_code", length = 30)
    val branchCode: String? = null,

    @SFField("PublicFlag__c")
    @HCColumn("publicflag__c")
    @Column(name = "public_flag")
    val publicFlag: Boolean? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
