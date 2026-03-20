package com.otoki.internal.notice.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*

@Entity
@Table(name = "notice")
@SFObject("DKRetail__Notice__c")
class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("EmployeeId__c")
    @HCColumn("employeeid__c")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("DKRetail__Scope__c")
    @HCColumn("dkretail__scope__c")
    @Column(name = "scope", length = 255)
    val scope: String? = null,

    @SFField("DKRetail__Category__c")
    @HCColumn("dkretail__category__c")
    @Column(name = "category", length = 255)
    @Convert(converter = NoticeCategoryConverter::class)
    var category: NoticeCategory? = null,

    @SFField("DKRetail__Contents__c")
    @HCColumn("dkretail__contents__c")
    @Column(name = "contents", columnDefinition = "TEXT")
    var contents: String? = null,

    @HCColumn("dkretail__educategory__c")
    @Column(name = "edu_category", length = 255)
    val eduCategory: String? = null,

    @SFField("DKRetail__Jeejum__c")
    @HCColumn("dkretail__jeejum__c")
    @Column(name = "branch", length = 255)
    var branch: String? = null,

    @SFField("DKRetail__JeejumCode__c")
    @HCColumn("dkretail__jeejumcode__c")
    @Column(name = "branch_code", length = 255)
    var branchCode: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

) : BaseEntity()
