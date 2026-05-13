package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 월별 여사원 통합일정 Entity
 */
@Entity
@Table(name = "monthly_female_employee_integration_schedule")
@SFObject("MonthlyFemaleEmployeeIntegrationSchedule__c")
@HCTable("monthlyfemaleemployeeintegrationschedule__c")
class MonthlyFemaleEmployeeIntegrationSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_female_employee_integration_schedule_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("ExternalKey__c")
    @HCColumn("externalkey__c")
    @Column(name = "external_key", length = 255, unique = true)
    val externalKey: String? = null,

    @SFField("Year__c")
    @HCColumn("year__c")
    @Column(name = "year", length = 255)
    val year: String? = null,

    @SFField("Month__c")
    @HCColumn("month__c")
    @Column(name = "month", length = 255)
    val month: String? = null,

    @SFField("Account__c")
    @HCColumn("account__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("FullName__c")
    @HCColumn("fullname__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 40)
    val costCenterCode: String? = null,

    @SFField("WorkingCategory1__c")
    @HCColumn("workingcategory1__c")
    @Column(name = "working_category1", length = 255)
    val workingCategory1: String? = null,

    @SFField("WorkingCategory3__c")
    @HCColumn("workingcategory3__c")
    @Column(name = "working_category3", length = 255)
    val workingCategory3: String? = null,

    @SFField("WorkingCategory4__c")
    @HCColumn("workingcategory4__c")
    @Column(name = "working_category4", length = 255)
    val workingCategory4: String? = null,

    @SFField("WorkingCategory5__c")
    @HCColumn("workingcategory5__c")
    @Column(name = "working_category5", length = 255)
    val workingCategory5: String? = null,

    @SFField("EmpBranchName__c")
    @HCColumn("empbranchname__c")
    @Column(name = "emp_branch_name", length = 255)
    val empBranchName: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @HCColumn("professionalpromotionteam__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "professional_promotion_team", length = 255)
    val professionalPromotionTeam: ProfessionalPromotionTeamType? = null,

    @SFField("WorkingDaysMonth__c")
    @HCColumn("workingdaysmonth__c")
    @Column(name = "working_days_month", precision = 14, scale = 4)
    val workingDaysMonth: BigDecimal? = null,

    @SFField("NumberOfInputs__c")
    @HCColumn("numberofinputs__c")
    @Column(name = "number_of_inputs")
    val numberOfInputs: Long? = null,

    @SFField("EquivalentNumberOfWorkingDays__c")
    @HCColumn("equivalentnumberofworkingdays__c")
    @Column(name = "equivalent_number_of_working_days", precision = 14, scale = 4)
    val equivalentNumberOfWorkingDays: BigDecimal? = null,

    @SFField("ConvertedHeadcount__c")
    @HCColumn("convertedheadcount__c")
    @Column(name = "converted_headcount", precision = 14, scale = 4)
    val convertedHeadcount: BigDecimal? = null,

    @SFField("EDI_POS__c")
    @HCColumn("edi_pos__c")
    @Column(name = "edi_pos")
    val ediPos: Long? = null,

    @SFField("ThisMonthAmount__c")
    @HCColumn("thismonthamount__c")
    @Column(name = "this_month_amount")
    val thisMonthAmount: Long? = null,

    @SFField("AccountConvertedHeadcount__c")
    @HCColumn("accountconvertedheadcount__c")
    @Column(name = "account_converted_headcount", precision = 14, scale = 4)
    val accountConvertedHeadcount: BigDecimal? = null,

    @SFField("EmployeeInputCriteriaMaster__c")
    @HCColumn("employeeinputcriteriamaster__c")
    @Column(name = "employee_input_criteria_master_sfid", length = 18)
    val employeeInputCriteriaMasterSfid: String? = null,

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
    val isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    val account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,

    // -- Spec #746 R-2 (EmployeeInputCriteriaMaster__c FK 신설) --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_input_criteria_master_id")
    var employeeInputCriteriaMaster: com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster? = null,

    // -- Spec #747 카테고리 A — D 분류 누락 --
    @SFField("DateForReport__c")
    @HCColumn("dateforreport__c")
    @Column(name = "date_for_report")
    var dateForReport: java.time.LocalDate? = null,

    // Spec #747: Year_Month__c (SF len=1300) — 기존 entity 의 `month` 컬럼이 H2 reserved word 와 schema 재검증 시 충돌
    // 발생 → 본 batch 보류. #750+ follow-up 으로 분리 검토 (기존 month 컬럼 quote 처리 + 신규 컬럼 추가).

) : BaseEntity()
