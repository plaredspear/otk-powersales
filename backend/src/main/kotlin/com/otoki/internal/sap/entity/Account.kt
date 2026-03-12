package com.otoki.internal.sap.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 거래처 마스터 Entity
 * Salesforce Account(거래처) 오브젝트 — SAP 거래처 마스터 동기화 대상 테이블.
 */
@Entity
@Table(name = "account")
@SFObject("Account")
@HCTable("account")
class Account(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 255)
    var name: String? = null,

    @SFField("Phone")
    @HCColumn("phone")
    @Column(name = "phone", length = 40)
    var phone: String? = null,

    @SFField("MobilePhone__c")
    @HCColumn("mobilephone__c")
    @Column(name = "mobile_phone", length = 40)
    var mobilePhone: String? = null,

    @SFField("Address1__c")
    @HCColumn("address1__c")
    @Column(name = "address1", length = 120)
    var address1: String? = null,

    @SFField("Address2__c")
    @HCColumn("address2__c")
    @Column(name = "address2", length = 120)
    var address2: String? = null,

    @SFField("Representative__c")
    @HCColumn("representative__c")
    @Column(name = "representative", length = 100)
    var representative: String? = null,

    @SFField("ABCType__c")
    @HCColumn("abctype__c")
    @Column(name = "abc_type", length = 20)
    var abcType: String? = null,

    @SFField("ABCTypeCode__c")
    @HCColumn("abctypecode__c")
    @Column(name = "abc_type_code", length = 40)
    var abcTypeCode: String? = null,

    @SFField("ExternalKey__c")
    @HCColumn("externalkey__c")
    @Column(name = "external_key", length = 100)
    var externalKey: String? = null,

    @SFField("AccountGroup__c")
    @HCColumn("accountgroup__c")
    @Column(name = "account_group", length = 10)
    var accountGroup: String? = null,

    @SFField("BranchCode__c")
    @HCColumn("branchcode__c")
    @Column(name = "branch_code", length = 100)
    var branchCode: String? = null,

    @SFField("BranchName__c")
    @HCColumn("branchname__c")
    @Column(name = "branch_name", length = 250)
    var branchName: String? = null,

    @SFField("Zipcode__c")
    @HCColumn("zipcode__c")
    @Column(name = "zip_code", length = 100)
    var zipCode: String? = null,

    @SFField("Latitude__c")
    @HCColumn("latitude__c")
    @Column(name = "latitude", length = 100)
    val latitude: String? = null,

    @SFField("Longitude__c")
    @HCColumn("longitude__c")
    @Column(name = "longitude", length = 100)
    val longitude: String? = null,

    @SFField("ClosingTime1__c")
    @HCColumn("closingtime1__c")
    @Column(name = "closing_time1", length = 50)
    var closingTime1: String? = null,

    @SFField("ClosingTime2__c")
    @HCColumn("closingtime2__c")
    @Column(name = "closing_time2", length = 50)
    var closingTime2: String? = null,

    @SFField("ClosingTime3__c")
    @HCColumn("closingtime3__c")
    @Column(name = "closing_time3", length = 50)
    var closingTime3: String? = null,

    @SFField("Industry")
    @HCColumn("industry")
    @Column(name = "industry", length = 255)
    var industry: String? = null,

    @SFField("WERK1_TX__c")
    @HCColumn("werk1_tx__c")
    @Column(name = "werk1_tx", length = 255)
    var werk1Tx: String? = null,

    @SFField("WERK2_TX__c")
    @HCColumn("werk2_tx__c")
    @Column(name = "werk2_tx", length = 255)
    var werk2Tx: String? = null,

    @SFField("WERK3_TX__c")
    @HCColumn("werk3_tx__c")
    @Column(name = "werk3_tx", length = 255)
    var werk3Tx: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // --- Spec #142: SAP 거래처 마스터 동기화 추가 컬럼 ---

    @Column(name = "account_type", length = 20)
    var accountType: String? = null,

    @Column(name = "account_status_code", length = 20)
    var accountStatusCode: String? = null,

    @Column(name = "account_status_name", length = 40)
    var accountStatusName: String? = null,

    @Column(name = "email", length = 255)
    var email: String? = null,

    @Column(name = "business_type", length = 100)
    var businessType: String? = null,

    @Column(name = "business_category", length = 100)
    var businessCategory: String? = null,

    @Column(name = "employee_code", length = 20)
    var employeeCode: String? = null,

    @Column(name = "business_license_number", length = 20)
    var businessLicenseNumber: String? = null,

    @Column(name = "division_code", length = 20)
    var divisionCode: String? = null,

    @Column(name = "division_name", length = 100)
    var divisionName: String? = null,

    @Column(name = "sales_dept_code", length = 20)
    var salesDeptCode: String? = null,

    @Column(name = "sales_dept_name", length = 100)
    var salesDeptName: String? = null,

    @Column(name = "distribution", length = 1)
    var distribution: String? = null,

    @Column(name = "consignment_acc", length = 1)
    var consignmentAcc: String? = null,

    @Column(name = "werk1", length = 10)
    var werk1: String? = null,

    @Column(name = "werk2", length = 10)
    var werk2: String? = null,

    @Column(name = "werk3", length = 10)
    var werk3: String? = null,

    @Column(name = "org_cd3", length = 20)
    var orgCd3: String? = null,

    @Column(name = "org_cd4", length = 20)
    var orgCd4: String? = null,

    @Column(name = "org_cd5", length = 20)
    var orgCd5: String? = null,

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
)
