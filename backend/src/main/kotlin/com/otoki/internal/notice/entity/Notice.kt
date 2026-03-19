package com.otoki.internal.notice.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*

@Entity
@Table(name = "dkretail__notice__c")
@SFObject("DKRetail__Notice__c")
@HCTable("dkretail__notice__c")
class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
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
    @Column(name = "dkretail__scope__c", length = 255)
    val scope: String? = null,

    @SFField("DKRetail__Category__c")
    @HCColumn("dkretail__category__c")
    @Column(name = "dkretail__category__c", length = 255)
    @Convert(converter = NoticeCategoryConverter::class)
    var category: NoticeCategory? = null,

    @SFField("DKRetail__Contents__c")
    @HCColumn("dkretail__contents__c")
    @Column(name = "dkretail__contents__c", columnDefinition = "TEXT")
    var contents: String? = null,

    @HCColumn("dkretail__educategory__c")
    @Column(name = "dkretail__educategory__c", length = 255)
    val eduCategory: String? = null,

    @SFField("DKRetail__Jeejum__c")
    @HCColumn("dkretail__jeejum__c")
    @Column(name = "dkretail__jeejum__c", length = 255)
    var branch: String? = null,

    @SFField("DKRetail__JeejumCode__c")
    @HCColumn("dkretail__jeejumcode__c")
    @Column(name = "dkretail__jeejumcode__c", length = 255)
    var branchCode: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "isdeleted")
    var isDeleted: Boolean? = null,

    @HCColumn("_hc_lastop")
    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @HCColumn("_hc_err")
    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null
) : BaseEntity()
