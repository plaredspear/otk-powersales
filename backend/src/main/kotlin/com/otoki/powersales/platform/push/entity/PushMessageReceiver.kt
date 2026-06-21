package com.otoki.powersales.platform.push.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * 푸시 메시지 수신자 Entity
 * V1 스키마: push_message_receiver (V43 PK/컬럼명 정리)
 * Spec #710 SF Object 정합 (Group A + Reference R-2)
 *
 * PushMessage N:1 관계 (PK 참조, DB FK 없음)
 * BaseEntity 미상속 — CreatedDate/LastModifiedDate 자체 컬럼 처리.
 */
@DomainName("푸시메시지 수신자")
@Entity
@Table(name = "push_message_receiver")
@SFObject("PushMessageReceiver__c")
class PushMessageReceiver(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_message_receiver_id")
    val id: Int = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @Column(name = "push_message_id")
    val pushMessageId: Int? = null,

    @SFField("MessageId__c")
    @Column(name = "push_message_sfid", length = 18)
    val pushMessageSfid: String? = null,

    // -- Spec #710: Group A — IsDeleted --
    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SFField("CreatedDate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @SFField("LastModifiedDate")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // -- Spec #710: Group A — CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (Heroku Connect / SalesforceMigrationTool 이 채움).
    // *_id: SF User → Employee 매핑 결과 FK.

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "push_message_id", insertable = false, updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    var pushMessage: PushMessage? = null,

    // V201 — SF PushMessageReceiver__c.CreatedById/LastModifiedById.referenceTo = [User].
    // audit FK Employee → User 정합 (V200 다른 entity 일괄 정합과 동일 패턴).
    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null

) : AuditedEntity()
