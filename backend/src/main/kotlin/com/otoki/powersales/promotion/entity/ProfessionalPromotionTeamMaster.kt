package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "professional_promotion_team_master")
@SFObject("ProfessionalPromotionTeamMaster__c")
class ProfessionalPromotionTeamMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "professional_promotion_team_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @SFField("EmployeeNumber__c")
    @Column(name = "employee_number", length = 20)
    val employeeNumber: String? = null,

    @Column(name = "account_id", nullable = false)
    var accountId: Int,

    @SFField("Account__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "team_type", nullable = false, length = 50)
    var teamType: ProfessionalPromotionTeamType,

    @SFField("StartDate__c")
    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @SFField("EndDate__c")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("Confirmed__c")
    @Column(name = "is_confirmed", nullable = false)
    var isConfirmed: Boolean = false,

    @SFField("CostCenterCode__c")
    @Column(name = "branch_code", length = 20)
    var branchCode: String? = null,

    @SFField("BranchName__c")
    @Column(name = "branch_name", length = 50)
    var branchName: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    val account: Account? = null

) : BaseEntity() {

    fun update(
        teamType: ProfessionalPromotionTeamType,
        startDate: LocalDate,
        endDate: LocalDate?,
        isConfirmed: Boolean,
        accountId: Int? = null
    ) {
        this.teamType = teamType
        this.startDate = startDate
        this.endDate = endDate
        this.isConfirmed = isConfirmed
        if (accountId != null) this.accountId = accountId
    }
}
