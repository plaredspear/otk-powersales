package com.otoki.internal.education.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 교육 조회 이력 Entity
 *
 * V1 테이블: education_view_history (구: education_member_history, PK 없음 → @IdClass 복합 키)
 */
@Entity
@Table(name = "education_view_history")
@IdClass(EducationViewHistoryId::class)
@HCTable("education_member_history")
class EducationViewHistory(

    @Id
    @HCColumn("community_id")
    @Column(name = "community_id", length = 20)
    val communityId: String,

    @Id
    @HCColumn("empcode__c")
    @Column(name = "empcode__c", length = 40)
    val empCode: String,

    @Id
    @HCColumn("inst_date")
    @Column(name = "inst_date")
    val instDate: LocalDateTime? = null,

    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("costcentercode__c")
    @Column(name = "costcentercode__c", length = 10)
    val costCenterCode: String? = null
)
