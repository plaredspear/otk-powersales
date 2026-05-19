package com.otoki.powersales.order.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "erp_order")
@SFObject("ERP_Order__c")
@HCTable("erp_order__c")
class ErpOrder(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "erp_order_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "sap_order_number", nullable = false, unique = true, length = 80)
    val sapOrderNumber: String,

    @SFField("SAPAccountCode__c")
    @Column(name = "sap_account_code", length = 100)
    var sapAccountCode: String? = null,

    @SFField("SAPAccountName__c")
    @Column(name = "sap_account_name", length = 100)
    var sapAccountName: String? = null,

    @SFField("DeliveryRequestDate__c")
    @Column(name = "delivery_request_date")
    var deliveryRequestDate: LocalDate? = null,

    @SFField("OrderDate__c")
    @Column(name = "order_date")
    var orderDate: LocalDate? = null,

    @SFField("EmployeeCode__c")
    @Column(name = "employee_code", length = 50)
    var employeeCode: String? = null,

    @SFField("EmployeeName__c")
    @Column(name = "employee_name", length = 100)
    var employeeName: String? = null,

    @SFField("TotalOrderAmount__c")
    @Column(name = "order_sales_amount")
    var orderSalesAmount: Long? = null,

    @SFField("OrderChannel__c")
    @Column(name = "order_channel", length = 10)
    var orderChannel: String? = null,

    @SFField("OrderChannel_NM__c")
    @Column(name = "order_channel_nm", length = 50)
    var orderChannelNm: String? = null,

    @SFField("OrderType__c")
    @Column(name = "order_type", length = 10)
    var orderType: String? = null,

    @SFField("OrderType_NM__c")
    @Column(name = "order_type_nm", length = 50)
    var orderTypeNm: String? = null,

    @SFField("AccountId__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: com.otoki.powersales.account.entity.Account? = null,
) : BaseEntity()
