package com.otoki.powersales.order.entity

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 주문요청 Entity (DKRetail__OrderRequest__c).
 * 영업사원이 거래처에 등록한 주문 요청서. SF managed package DKRetail 의 주문요청 객체에 매핑된다.
 */
@Entity
@Table(
    name = "order_request",
    indexes = [
        Index(name = "idx_order_request_employee_id", columnList = "employee_id"),
        Index(name = "idx_order_request_account_id", columnList = "account_id"),
        Index(name = "idx_order_request_order_date", columnList = "order_date"),
        Index(name = "idx_order_request_delivery_date", columnList = "delivery_date"),
        Index(name = "idx_order_request_order_request_status", columnList = "order_request_status"),
    ],
)
@SFObject("DKRetail__OrderRequest__c")
@HCTable("dkretail__orderrequest__c")
class OrderRequest(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_request_id")
    val id: Long = 0,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "order_request_number", nullable = false, unique = true, length = 80)
    val orderRequestNumber: String,

    /**
     * 모바일 등록 멱등키 (Spec #592). 동일 키 재요청 시 1차 row 의 응답을 그대로 반환.
     * `idx_order_request_client_request_id_unique` partial unique 인덱스 (`WHERE NOT NULL`).
     */
    @Column(name = "client_request_id", length = 64)
    val clientRequestId: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @HCColumn("dkretail__employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__AccountId__c")
    @HCColumn("dkretail__accountid__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("OrderDate__c")
    @HCColumn("orderdate__c")
    @Column(name = "order_date", nullable = false)
    val orderDate: LocalDateTime,

    @SFField("DKRetail__OrderDate__c")
    @HCColumn("dkretail__orderdate__c")
    @Column(name = "dk_order_date")
    var dkOrderDate: LocalDate? = null,

    @SFField("DKRetail__RequestDate__c")
    @HCColumn("dkretail__requestdate__c")
    @Column(name = "delivery_date", nullable = false)
    val deliveryDate: LocalDate,

    @SFField("TotalOrderAmount__c")
    @HCColumn("totalorderamount__c")
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    val totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_approved_amount", nullable = false, precision = 18, scale = 2)
    var totalApprovedAmount: BigDecimal = BigDecimal.ZERO,

    @SFField("DKRetail__RequestStatus__c")
    @HCColumn("dkretail__requeststatus__c")
    @Column(name = "order_request_status", nullable = false, length = 255)
    @Convert(converter = OrderRequestStatusConverter::class)
    var orderRequestStatus: OrderRequestStatus = OrderRequestStatus.DRAFT,

    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = false,

    @Column(name = "client_deadline_time", length = 5)
    val clientDeadlineTime: String? = null,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    val employee: Employee,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,
) : BaseEntity()
