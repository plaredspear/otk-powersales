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

    @Column(name = "sfid", length = 18)
    @HCColumn("sfid")
    val sfid: String? = null,

    @Column(name = "name", length = 255)
    @SFField("Name")
    @HCColumn("name")
    var name: String? = null,

    @Column(name = "phone", length = 40)
    @SFField("Phone")
    @HCColumn("phone")
    var phone: String? = null,

    @Column(name = "mobile_phone", length = 40)
    @SFField("MobilePhone__c")
    @HCColumn("mobilephone__c")
    var mobilePhone: String? = null,

    @Column(name = "address1", length = 120)
    @SFField("Address1__c")
    @HCColumn("address1__c")
    var address1: String? = null,

    @Column(name = "address2", length = 120)
    @SFField("Address2__c")
    @HCColumn("address2__c")
    var address2: String? = null,

    @Column(name = "representative", length = 100)
    @SFField("Representative__c")
    @HCColumn("representative__c")
    var representative: String? = null,

    @Column(name = "abc_type", length = 20)
    @SFField("ABCType__c")
    @HCColumn("abctype__c")
    var abcType: String? = null,

    @Column(name = "abc_type_code", length = 40)
    @SFField("ABCTypeCode__c")
    @HCColumn("abctypecode__c")
    var abcTypeCode: String? = null,

    @Column(name = "external_key", length = 100)
    @SFField("ExternalKey__c")
    @HCColumn("externalkey__c")
    var externalKey: String? = null,

    @Column(name = "account_group", length = 10)
    @SFField("AccountGroup__c")
    @HCColumn("accountgroup__c")
    var accountGroup: String? = null,

    @Column(name = "branch_code", length = 100)
    @SFField("BranchCode__c")
    @HCColumn("branchcode__c")
    var branchCode: String? = null,

    @Column(name = "branch_name", length = 250)
    @SFField("BranchName__c")
    @HCColumn("branchname__c")
    var branchName: String? = null,

    @Column(name = "zip_code", length = 100)
    @SFField("Zipcode__c")
    @HCColumn("zipcode__c")
    var zipCode: String? = null,

    @Column(name = "latitude", length = 100)
    @SFField("Latitude__c")
    @HCColumn("latitude__c")
    val latitude: String? = null,

    @Column(name = "longitude", length = 100)
    @SFField("Longitude__c")
    @HCColumn("longitude__c")
    val longitude: String? = null,

    @Column(name = "closing_time1", length = 50)
    @SFField("ClosingTime1__c")
    @HCColumn("closingtime1__c")
    var closingTime1: String? = null,

    @Column(name = "closing_time2", length = 50)
    @SFField("ClosingTime2__c")
    @HCColumn("closingtime2__c")
    var closingTime2: String? = null,

    @Column(name = "closing_time3", length = 50)
    @SFField("ClosingTime3__c")
    @HCColumn("closingtime3__c")
    var closingTime3: String? = null,

    @Column(name = "industry", length = 255)
    @SFField("Industry")
    @HCColumn("industry")
    var industry: String? = null,

    @Column(name = "werk1_tx", length = 255)
    @SFField("WERK1_TX__c")
    @HCColumn("werk1_tx__c")
    var werk1Tx: String? = null,

    @Column(name = "werk2_tx", length = 255)
    @SFField("WERK2_TX__c")
    @HCColumn("werk2_tx__c")
    var werk2Tx: String? = null,

    @Column(name = "werk3_tx", length = 255)
    @SFField("WERK3_TX__c")
    @HCColumn("werk3_tx__c")
    var werk3Tx: String? = null,

    @Column(name = "is_deleted")
    @HCColumn("isdeleted")
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
