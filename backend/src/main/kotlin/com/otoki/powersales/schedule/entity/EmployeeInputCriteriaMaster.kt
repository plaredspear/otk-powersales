package com.otoki.powersales.schedule.entity

import com.otoki.powersales.account.entity.AccountCategoryMaster
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.converter.TypeOfWork1Converter
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 여사원 투입 기준 마스터 Entity (EmployeeInputCriteriaMaster__c).
 * MonthlyFemaleEmployeeIntegrationSchedule 의 인사 투입 기준 source.
 */
@Entity
@Table(name = "employee_input_criteria_master")
@SFObject("EmployeeInputCriteriaMaster__c")
@HCTable("employeeinputcriteriamaster__c")
class EmployeeInputCriteriaMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_input_criteria_master_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("BifurcationHalfPersonStandard__c")
    @HCColumn("bifurcationhalfpersonstandard__c")
    @Column(name = "bifurcation_half_person_standard", precision = 18, scale = 0)
    var bifurcationHalfPersonStandard: BigDecimal? = null,

    @SFField("Boundary__c")
    @HCColumn("boundary__c")
    @Column(name = "boundary", precision = 18, scale = 0)
    var boundary: BigDecimal? = null,

    @SFField("Category__c")
    @HCColumn("category__c")
    @Column(name = "category_sfid", length = 18)
    var categorySfid: String? = null,

    @SFField("ConfirmAlert__c")
    @HCColumn("confirmalert__c")
    @Column(name = "confirm_alert", length = 1300)
    var confirmAlert: String? = null,

    @SFField("Confirmed__c")
    @HCColumn("confirmed__c")
    @Column(name = "confirmed", nullable = false)
    var confirmed: Boolean = false,

    @SFField("StartDate__c")
    @HCColumn("startdate__c")
    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @HCColumn("enddate__c")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("Fixed1PersonStandardAmount__c")
    @HCColumn("fixed1personstandardamount__c")
    @Column(name = "fixed_1_person_standard_amount", precision = 18, scale = 0)
    var fixed1PersonStandardAmount: BigDecimal? = null,

    @SFField("TypeOfWork1__c")
    @HCColumn("typeofwork1__c")
    @Convert(converter = TypeOfWork1Converter::class)
    @Column(name = "type_of_work_1", length = 255)
    var typeOfWork1: TypeOfWork1? = null,

    @SFField("AccountCategorizedCode__c")
    @HCColumn("accountcategorizedcode__c")
    @Column(name = "account_categorized_code", length = 1300)
    var accountCategorizedCode: String? = null,

    @SFField("BifurcationHalfPersonMinAmountInRealmRan__c")
    @HCColumn("bifurcationhalfpersonminamountinrealmran__c")
    @Column(name = "bifurcation_half_person_min_amount_in_realm_ran", precision = 18, scale = 0)
    var bifurcationHalfPersonMinAmountInRealmRan: BigDecimal? = null,

    @SFField("Fixed1PersonMinAmountInRealmRange__c")
    @HCColumn("fixed1personminamountinrealmrange__c")
    @Column(name = "fixed_1_person_min_amount_in_realm_range", precision = 18, scale = 0)
    var fixed1PersonMinAmountInRealmRange: BigDecimal? = null,

    @SFField("ValidData__c")
    @HCColumn("validdata__c")
    @Column(name = "valid_data", length = 1300)
    var validData: String? = null,

    @SFField("Valid__c")
    @HCColumn("valid__c")
    @Column(name = "valid", length = 1300)
    var valid: String? = null,

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
    @JoinColumn(name = "category_id")
    var category: AccountCategoryMaster? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,
) : BaseEntity()
