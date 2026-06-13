package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.common.entity.OwnerUserDefaultListener

/**
 * 월별 여사원 통합일정 Entity
 */
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "monthly_female_employee_integration_schedule")
@SFObject("MonthlyFemaleEmployeeIntegrationSchedule__c")
class MonthlyFemaleEmployeeIntegrationSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_female_employee_integration_schedule_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("ExternalKey__c")
    @Column(name = "external_key", length = 255, unique = true)
    val externalKey: String? = null,

    @SFField("Year__c")
    @Column(name = "year", length = 255)
    val year: String? = null,

    @SFField("Month__c")
    @Column(name = "month", length = 255)
    val month: String? = null,

    @SFField("Account__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("FullName__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("CostCenterCode__c")
    @Column(name = "cost_center_code", length = 40)
    val costCenterCode: String? = null,

    @SFField("WorkingCategory1__c")
    @Column(name = "working_category1", length = 255)
    val workingCategory1: String? = null,

    @SFField("WorkingCategory3__c")
    @Column(name = "working_category3", length = 255)
    val workingCategory3: String? = null,

    @SFField("WorkingCategory4__c")
    @Column(name = "working_category4", length = 255)
    val workingCategory4: String? = null,

    @SFField("WorkingCategory5__c")
    @Column(name = "working_category5", length = 255)
    val workingCategory5: String? = null,

    @SFField("EmpBranchName__c")
    @Column(name = "emp_branch_name", length = 255)
    val empBranchName: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "professional_promotion_team", length = 255)
    val professionalPromotionTeam: ProfessionalPromotionTeamType? = null,

    @SFField("WorkingDaysMonth__c")
    @Column(name = "working_days_month", precision = 18, scale = 4)
    val workingDaysMonth: BigDecimal? = null,

    @SFField("NumberOfInputs__c")
    @Column(name = "number_of_inputs")
    val numberOfInputs: BigDecimal? = null,

    @SFField("EquivalentNumberOfWorkingDays__c")
    @Column(name = "equivalent_number_of_working_days", precision = 18, scale = 4)
    val equivalentNumberOfWorkingDays: BigDecimal? = null,

    @SFField("ConvertedHeadcount__c")
    @Column(name = "converted_headcount", precision = 18, scale = 4)
    val convertedHeadcount: BigDecimal? = null,

    @SFField("EDI_POS__c")
    @Column(name = "edi_pos")
    val ediPos: BigDecimal? = null,

    @SFField("ThisMonthAmount__c")
    @Column(name = "this_month_amount")
    var thisMonthAmount: BigDecimal? = null,

    @SFField("AccountConvertedHeadcount__c")
    @Column(name = "account_converted_headcount", precision = 18, scale = 4)
    var accountConvertedHeadcount: BigDecimal? = null,

    @SFField("EmployeeInputCriteriaMaster__c")
    @Column(name = "employee_input_criteria_master_sfid", length = 18)
    var employeeInputCriteriaMasterSfid: String? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    val account: Account? = null,

    // OwnerId polymorphic [Group, User] — owner_sfid sync buffer + (owner_user XOR owner_group) FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_input_criteria_master_id")
    var employeeInputCriteriaMaster: com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster? = null,

) : BaseEntity()
