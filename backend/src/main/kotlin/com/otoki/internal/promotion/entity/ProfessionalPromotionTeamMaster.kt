package com.otoki.internal.promotion.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "professional_promotion_team_master")
@SFObject("ProfessionalPromotionTeamMaster__c")
@HCTable("professionalpromotionteammaster__c")
class ProfessionalPromotionTeamMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "professional_promotion_team_master_id")
    val id: Long = 0,

    @SFField("EmployeeNumber__c")
    @HCColumn("employeenumber__c")
    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("Account__c")
    @HCColumn("account__c")
    @Column(name = "account_id", nullable = false)
    val accountId: Int,

    @SFField("ProfessionalPromotionTeam__c")
    @HCColumn("professionalpromotionteam__c")
    @Column(name = "team_type", nullable = false, length = 50)
    var teamType: String,

    @SFField("StartDate__c")
    @HCColumn("startdate__c")
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @SFField("EndDate__c")
    @HCColumn("enddate__c")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("Confirmed__c")
    @HCColumn("confirmed__c")
    @Column(name = "is_confirmed", nullable = false)
    var isConfirmed: Boolean = false,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "branch_code", length = 20)
    var branchCode: String? = null,

    @SFField("BranchName__c")
    @HCColumn("branchname__c")
    @Column(name = "branch_name", length = 50)
    var branchName: String? = null

) : BaseEntity() {

    fun update(
        teamType: String,
        startDate: LocalDate,
        endDate: LocalDate?,
        isConfirmed: Boolean,
        accountId: Int? = null
    ) {
        this.teamType = teamType
        this.startDate = startDate
        this.endDate = endDate
        this.isConfirmed = isConfirmed
    }
}
