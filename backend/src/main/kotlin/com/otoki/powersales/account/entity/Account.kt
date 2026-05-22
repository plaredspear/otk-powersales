package com.otoki.powersales.account.entity

import com.otoki.powersales.account.entity.converter.AccountSourceConverter
import com.otoki.powersales.account.entity.converter.AccountTypeConverter
import com.otoki.powersales.account.entity.converter.FreezerTypeConverter
import com.otoki.powersales.account.entity.converter.IndustryConverter
import com.otoki.powersales.account.entity.converter.OwnershipConverter
import com.otoki.powersales.account.entity.converter.RatingConverter
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

/**
 * 거래처 마스터 Entity
 * Salesforce Account(거래처) 오브젝트 — SAP 거래처 마스터 동기화 대상 테이블.
 */
@Entity
@Table(name = "account")
@SFObject("Account")
class Account(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 255)
    var name: String? = null,

    @SFField("Phone")
    @Column(name = "phone", length = 40)
    var phone: String? = null,

    @SFField("MobilePhone__c")
    @Column(name = "mobile_phone", length = 40)
    var mobilePhone: String? = null,

    @SFField("Address1__c")
    @Column(name = "address1", length = 120)
    var address1: String? = null,

    @SFField("Address2__c")
    @Column(name = "address2", length = 120)
    var address2: String? = null,

    @SFField("Representative__c")
    @Column(name = "representative", length = 100)
    var representative: String? = null,

    @SFField("ABCType__c")
    @Column(name = "abc_type", length = 20)
    var abcType: String? = null,

    @SFField("ABCTypeCode__c")
    @Column(name = "abc_type_code", length = 40)
    var abcTypeCode: String? = null,

    @SFField("ExternalKey__c")
    @Column(name = "external_key", unique = true, length = 100)
    var externalKey: String? = null,

    @SFField("AccountGroup__c")
    @Column(name = "account_group", length = 10)
    var accountGroup: String? = null,

    @SFField("BranchCode__c")
    @Column(name = "branch_code", length = 100)
    var branchCode: String? = null,

    @SFField("BranchName__c")
    @Column(name = "branch_name", length = 250)
    var branchName: String? = null,

    @SFField("Zipcode__c")
    @Column(name = "zip_code", length = 100)
    var zipCode: String? = null,

    @SFField("Latitude__c")
    @Column(name = "latitude", length = 100)
    var latitude: String? = null,

    @SFField("Longitude__c")
    @Column(name = "longitude", length = 100)
    var longitude: String? = null,

    @SFField("ClosingTime1__c")
    @Column(name = "closing_time1", length = 50)
    var closingTime1: String? = null,

    @SFField("ClosingTime2__c")
    @Column(name = "closing_time2", length = 50)
    var closingTime2: String? = null,

    @SFField("ClosingTime3__c")
    @Column(name = "closing_time3", length = 50)
    var closingTime3: String? = null,

    @SFField("Industry")
    @Convert(converter = IndustryConverter::class)
    @Column(name = "industry", length = 255)
    var industry: Industry? = null,

    @SFField("WERK1_TX__c")
    @Column(name = "werk1_tx", length = 255)
    var werk1Tx: String? = null,

    @SFField("WERK2_TX__c")
    @Column(name = "werk2_tx", length = 255)
    var werk2Tx: String? = null,

    @SFField("WERK3_TX__c")
    @Column(name = "werk3_tx", length = 255)
    var werk3Tx: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @SFField("Type")
    @Convert(converter = AccountTypeConverter::class)
    @Column(name = "account_type", length = 255)
    var accountType: AccountType? = null,

    @SFField("AccountStatusName__c")
    @Column(name = "account_status_name", length = 40)
    var accountStatusName: String? = null,

    @SFField("EmployeeCode__c")
    @Column(name = "employee_code", length = 15)
    var employeeCode: String? = null,

    @SFField("Distribution__c")
    @Column(name = "distribution", length = 20)
    var distribution: String? = null,

    @SFField("AccountStatusCode__c")
    @Column(name = "account_status_code", length = 100)
    var accountStatusCode: String? = null,

    @SFField("BusinessType__c")
    @Column(name = "business_type", length = 100)
    var businessType: String? = null,

    @SFField("BusinessCategory__c")
    @Column(name = "business_category", length = 100)
    var businessCategory: String? = null,

    @SFField("Sic")
    @Column(name = "business_license_number", length = 20)
    var businessLicenseNumber: String? = null,

    @SFField("Email__c")
    @Column(name = "email", length = 241)
    var email: String? = null,

    @SFField("DivisionName__c")
    @Column(name = "division_name", length = 250)
    var divisionName: String? = null,

    @SFField("SalesDeptName__c")
    @Column(name = "sales_dept_name", length = 250)
    var salesDeptName: String? = null,

    @SFField("ConsignmentAcc__c")
    @Column(name = "consignment_acc", length = 40)
    var consignmentAcc: String? = null,

    @SFField("WERK1__c")
    @Column(name = "werk1", length = 255)
    var werk1: String? = null,

    @SFField("WERK2__c")
    @Column(name = "werk2", length = 255)
    var werk2: String? = null,

    @SFField("WERK3__c")
    @Column(name = "werk3", length = 255)
    var werk3: String? = null,

    @SFField("SalesDeptCostCenter__c")
    @Column(name = "sales_dept_cost_center", length = 50)
    var salesDeptCostCenter: String? = null,

    @SFField("DivisionCostCenter__c")
    @Column(name = "division_cost_center", length = 50)
    var divisionCostCenter: String? = null,

    // -- Spec #602: SF 누락 컬럼 신규 도입 (Q1 옵션 1 + Q4 추가 → 총 24개) --

    @SFField("AccountNumber")
    @Column(name = "account_number", length = 40)
    var accountNumber: String? = null,

    @SFField("Site")
    @Column(name = "site", length = 80)
    var site: String? = null,

    @SFField("AccountSource")
    @Convert(converter = AccountSourceConverter::class)
    @Column(name = "account_source", length = 255)
    var accountSource: AccountSource? = null,

    @SFField("BranchCostCenter__c")
    @Column(name = "branch_cost_center", length = 50)
    var branchCostCenter: String? = null,

    @SFField("DivisionCode__c")
    @Column(name = "division_code", length = 100)
    var divisionCode: String? = null,

    @SFField("SalesDeptCode__c")
    @Column(name = "sales_dept_code", length = 100)
    var salesDeptCode: String? = null,

    @SFField("LogisticsName__c")
    @Column(name = "logistics_name", length = 50)
    var logisticsName: String? = null,

    @SFField("LogisticsCode__c")
    @Column(name = "logistics_code", length = 50)
    var logisticsCode: String? = null,

    @SFField("FreezerInstalled__c")
    @Column(name = "freezer_installed")
    var freezerInstalled: Boolean? = null,

    @SFField("FreezerType__c")
    @Convert(converter = FreezerTypeConverter::class)
    @Column(name = "freezer_type", length = 255)
    var freezerType: FreezerType? = null,

    @SFField("Field1__c")
    @Column(name = "remaining_credit", precision = 18, scale = 0)
    var remainingCredit: BigDecimal? = null,

    @SFField("TotalCredit__c")
    @Column(name = "total_credit", precision = 18, scale = 0)
    var totalCredit: BigDecimal? = null,

    @SFField("MapCoordinate__c")
    @Column(name = "map_coordinate", length = 40)
    var mapCoordinate: String? = null,

    @SFField("OrderEndTime__c")
    @Column(name = "order_end_time")
    var orderEndTime: LocalTime? = null,

    @SFField("FirstInstalled__c")
    @Column(name = "first_installed")
    var firstInstalled: LocalDate? = null,

    @SFField("Description")
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    @SFField("Website")
    @Column(name = "website", length = 255)
    var website: String? = null,

    @SFField("Fax")
    @Column(name = "fax", length = 40)
    var fax: String? = null,

    @SFField("AnnualRevenue")
    @Column(name = "annual_revenue", precision = 18, scale = 0)
    var annualRevenue: BigDecimal? = null,

    @SFField("NumberOfEmployees")
    @Column(name = "number_of_employees")
    var numberOfEmployees: BigDecimal? = null,

    @SFField("ParentId")
    @Column(name = "parent_sfid", length = 18)
    var parentSfid: String? = null,

    @SFField("Rating")
    @Convert(converter = RatingConverter::class)
    @Column(name = "rating", length = 255)
    var rating: Rating? = null,

    @SFField("Ownership")
    @Convert(converter = OwnershipConverter::class)
    @Column(name = "ownership", length = 255)
    var ownership: Ownership? = null,

    @SFField("IsPriorityRecord")
    @Column(name = "is_priority_record")
    var isPriorityRecord: Boolean? = null,

    // -- Spec #758: Audit FK 타입 Employee → User 전환 (Account 단독 구현분) --
    // SF sobject 메타 정합 (referenceTo == User). application 코드는 FK 컬럼만 사용, sfid 직접 JOIN 금지.

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Account? = null
) : BaseEntity()
