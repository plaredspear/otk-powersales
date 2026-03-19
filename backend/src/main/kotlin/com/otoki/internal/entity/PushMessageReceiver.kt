package com.otoki.internal.entity

import com.otoki.internal.common.entity.BaseEntity
import jakarta.persistence.*

/**
 * 푸시 메시지 수신자 Entity
 * V1 스키마: push_message_receiver (V42 리네이밍)
 *
 * PushMessage N:1 관계 (PK 참조, DB FK 없음)
 */
@Entity
@Table(name = "push_message_receiver")
class PushMessageReceiver(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @Column(name = "message_id")
    val messageId: Int? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null
) : BaseEntity()
