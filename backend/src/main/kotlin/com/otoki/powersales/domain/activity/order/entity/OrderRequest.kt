package com.otoki.powersales.domain.activity.order.entity

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
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
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import jakarta.persistence.EntityListeners
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 주문요청 Entity (DKRetail__OrderRequest__c).
 * 영업사원이 거래처에 등록한 주문 요청서. SF managed package DKRetail 의 주문요청 객체에 매핑된다.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("주문요청")
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
class OrderRequest(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("주문요청ID")
    @Column(name = "order_request_id")
    val id: Long = 0,

    @SFField("Name")
    @FieldName("주문요청번호")
    @Column(name = "order_request_number", nullable = false, unique = true, length = 80)
    val orderRequestNumber: String,

    /**
     * 모바일 등록 멱등키 (Spec #592). 동일 키 재요청 시 1차 row 의 응답을 그대로 반환.
     * `idx_order_request_client_request_id_unique` partial unique 인덱스 (`WHERE NOT NULL`).
     */
    @FieldName("클라이언트요청ID")
    @Column(name = "client_request_id", length = 64)
    val clientRequestId: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__AccountId__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("OrderDate__c")
    @FieldName("주문일시")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "order_date")
    val orderDate: LocalDateTime? = null,

    @SFField("DKRetail__OrderDate__c")
    @FieldName("주문일시(DK)")
    @Column(name = "dk_order_date")
    var dkOrderDate: LocalDate? = null,

    @SFField("DKRetail__RequestDate__c")
    @FieldName("납기일")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "delivery_date")
    val deliveryDate: LocalDate? = null,

    @SFField("TotalOrderAmount__c")
    @FieldName("총주문금액 (원)")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "total_amount", precision = 18, scale = 2)
    val totalAmount: BigDecimal? = BigDecimal.ZERO,

    @FieldName("총승인금액")
    @Column(name = "total_approved_amount", precision = 18, scale = 2)
    var totalApprovedAmount: BigDecimal? = BigDecimal.ZERO,

    @SFField("DKRetail__RequestStatus__c")
    @FieldName("상태")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "order_request_status", length = 255)
    @Convert(converter = OrderRequestStatusConverter::class)
    var orderRequestStatus: OrderRequestStatus? = OrderRequestStatus.DRAFT,

    /**
     * SAP 등록 확정 거부 사유 (SAP `resutlMsg` 원문). 비동기 outbox 송신에서 SAP 가 명시적으로 거부
     * (`resultCode` ≠ `"S"`)해 `SEND_FAILED` 로 전이될 때 기록한다 — 사용자가 상세에서 실패 사유를 확인하도록.
     * 재전송/재요청으로 다시 `SENT` 로 복귀할 때 null 로 초기화한다. 일시적 장애(재시도 소진)로 인한 실패는
     * SAP 업무 사유가 아니므로 여기에 담지 않는다(사유 원문이 없음). 신규 컬럼이라 SF 매핑 없음.
     */
    @FieldName("전송실패사유")
    @Column(name = "send_fail_reason", columnDefinition = "TEXT")
    var sendFailReason: String? = null,

    @FieldName("마감여부")
    @Column(name = "is_closed", nullable = false)
    var isClosed: Boolean = false,

    @FieldName("마감시각")
    @Column(name = "client_deadline_time", length = 5)
    val clientDeadlineTime: String? = null,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    val account: Account? = null,

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
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

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
) : BaseEntity()
