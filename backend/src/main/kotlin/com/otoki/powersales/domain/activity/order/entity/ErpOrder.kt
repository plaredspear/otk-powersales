package com.otoki.powersales.domain.activity.order.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@DomainName("ERP주문")
@Entity
@Table(name = "erp_order")
@SFObject("ERP_Order__c")
class ErpOrder(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("ERP주문ID")
    @Column(name = "erp_order_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("ERP 주문 Key값(Back-End 용)")
    @Column(name = "sap_order_number", nullable = false, unique = true, length = 80)
    val sapOrderNumber: String,

    @SFField("SAPAccountCode__c")
    @FieldName("거래처코드")
    @Column(name = "sap_account_code", length = 100)
    var sapAccountCode: String? = null,

    @SFField("SAPAccountName__c")
    @FieldName("SAP거래처명")
    @Column(name = "sap_account_name", length = 100)
    var sapAccountName: String? = null,

    @SFField("DeliveryRequestDate__c")
    @FieldName("납기일")
    @Column(name = "delivery_request_date")
    var deliveryRequestDate: LocalDate? = null,

    @SFField("OrderDate__c")
    @FieldName("주문생성일")
    @Column(name = "order_date")
    var orderDate: LocalDate? = null,

    @SFField("EmployeeCode__c")
    @FieldName("주문자사번")
    @Column(name = "employee_code", length = 50)
    var employeeCode: String? = null,

    @SFField("EmployeeName__c")
    @FieldName("주문자명")
    @Column(name = "employee_name", length = 100)
    var employeeName: String? = null,

    @SFField("TotalOrderAmount__c")
    @FieldName("총주문금액 (원)")
    @Column(name = "order_sales_amount")
    var orderSalesAmount: BigDecimal? = null,

    @SFField("OrderChannel__c")
    @FieldName("접수채널 코드")
    @Column(name = "order_channel", length = 10)
    var orderChannel: String? = null,

    @SFField("OrderChannel_NM__c")
    @FieldName("접수채널 명")
    @Column(name = "order_channel_nm", length = 50)
    var orderChannelNm: String? = null,

    @SFField("OrderType__c")
    @FieldName("주문유형 코드")
    @Column(name = "order_type", length = 10)
    var orderType: String? = null,

    @SFField("OrderType_NM__c")
    @FieldName("주문유형 명")
    @Column(name = "order_type_nm", length = 50)
    var orderTypeNm: String? = null,

    @SFField("AccountId__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,
) : BaseEntity()
