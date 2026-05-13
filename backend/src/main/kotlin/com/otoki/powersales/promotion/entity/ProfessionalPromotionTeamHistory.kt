package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "professional_promotion_team_history")
@SFObject("ProfessionalPromotionTeamHistory__c")
@HCTable("professionalpromotionteamhistory__c")
class ProfessionalPromotionTeamHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "professional_promotion_team_history_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("EmployeeId__c")
    @HCColumn("employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("oldValue__c")
    @HCColumn("oldvalue__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "old_value", length = 255)
    val oldValue: ProfessionalPromotionTeamType? = null,

    @SFField("newValue__c")
    @HCColumn("newvalue__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "new_value", nullable = false, length = 255)
    val newValue: ProfessionalPromotionTeamType,

    @SFField("updateTime__c")
    @HCColumn("updatetime__c")
    @Column(name = "changed_at", nullable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(),

    // -- Group A R-2: Owner / CreatedBy / LastModifiedBy --
    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,

    // -- Spec #747 카테고리 A Q2 — history 보존 의미의 사원코드 캐시 --
    @SFField("empCode__c")
    @HCColumn("empcode__c")
    @Column(name = "emp_code", length = 1300)
    var empCode: String? = null,
) : BaseEntity()
