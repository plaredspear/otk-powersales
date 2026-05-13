package com.otoki.powersales.order.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*

@Entity
@Table(name = "erp_order")
@SFObject("ERP_Order__c")
@HCTable("erp_order__c")
class ErpOrder(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "erp_order_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "sap_order_number", nullable = false, unique = true, length = 20)
    val sapOrderNumber: String,

    @SFField("SAPAccountCode__c")
    @Column(name = "sap_account_code", length = 20)
    var sapAccountCode: String? = null,

    @SFField("SAPAccountName__c")
    @Column(name = "sap_account_name", length = 100)
    var sapAccountName: String? = null,

    @SFField("DeliveryRequestDate__c")
    @Column(name = "delivery_request_date", length = 8)
    var deliveryRequestDate: String? = null,

    @SFField("OrderDate__c")
    @Column(name = "order_date", length = 8)
    var orderDate: String? = null,

    @SFField("EmployeeCode__c")
    @Column(name = "employee_code", length = 20)
    var employeeCode: String? = null,

    @SFField("EmployeeName__c")
    @Column(name = "employee_name", length = 50)
    var employeeName: String? = null,

    @SFField("TotalOrderAmount__c")
    @Column(name = "order_sales_amount")
    var orderSalesAmount: Double? = null,

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
