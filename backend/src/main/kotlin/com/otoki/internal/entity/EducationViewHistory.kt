package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 교육 조회 이력 Entity
 *
 * V1 테이블: education_member_history (PK 없음 → @IdClass 복합 키)
 */
@Entity
@Table(name = "education_member_history")
@IdClass(EducationViewHistoryId::class)
class EducationViewHistory(

    @Id
    @Column(name = "community_id", length = 20)
    val communityId: String,

    @Id
    @Column(name = "empcode__c", length = 40)
    val empCode: String,

    @Id
    @Column(name = "inst_date")
    val instDate: LocalDateTime? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "costcentercode__c", length = 10)
    val costCenterCode: String? = null
)
