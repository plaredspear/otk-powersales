package com.otoki.internal.safetycheck.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "safetycheck__workschedule__member")
@HCTable("safetycheck__workschedule__member")
@IdClass(SafetyCheckMemberId::class)
class SafetyCheckSubmission(

    @Id
    @Column(name = "\"masterId\"", length = 18)
    @HCColumn("masterId")
    val masterId: String = "",

    @Id
    @Column(name = "employeeid__c", length = 18)
    @HCColumn("employeeid__c")
    val employeeId: String = "",

    @Id
    @Column(name = "working__date")
    @HCColumn("working__date")
    val workingDate: LocalDate? = null,

    @Column(name = "starttime")
    @HCColumn("starttime")
    val startTime: LocalDateTime? = null,

    @Column(name = "completetime")
    @HCColumn("completetime")
    val completeTime: LocalDateTime? = null,

    @Column(name = "yes_chkcnt")
    @HCColumn("yes_chkcnt")
    val yesChkCnt: Double? = null,

    @Column(name = "no_chkcnt")
    @HCColumn("no_chkcnt")
    val noChkCnt: Double? = null,

    @Column(name = "equipment1", length = 10)
    @HCColumn("equipment1")
    val equipment1: String? = null,

    @Column(name = "equipment2", length = 10)
    @HCColumn("equipment2")
    val equipment2: String? = null,

    @Column(name = "equipment3", length = 10)
    @HCColumn("equipment3")
    val equipment3: String? = null,

    @Column(name = "equipment4", length = 10)
    @HCColumn("equipment4")
    val equipment4: String? = null,

    @Column(name = "equipment5", length = 10)
    @HCColumn("equipment5")
    val equipment5: String? = null,

    @Column(name = "equipment6", length = 10)
    @HCColumn("equipment6")
    val equipment6: String? = null,

    @Column(name = "equipment7", length = 10)
    @HCColumn("equipment7")
    val equipment7: String? = null,

    @Column(name = "equipment8", length = 10)
    @HCColumn("equipment8")
    val equipment8: String? = null,

    @Column(name = "equipment9", length = 10)
    @HCColumn("equipment9")
    val equipment9: String? = null,

    @Column(name = "precaution", length = 3000)
    @HCColumn("precaution")
    val precaution: String? = null,

    @Column(name = "precaution_chkcnt")
    @HCColumn("precaution_chkcnt")
    val precautionChkCnt: Double? = null,

    @Column(name = "traversalflag", length = 255)
    @HCColumn("traversalflag")
    val traversalFlag: String? = null,

    @Column(name = "eventmasterid", length = 18)
    @HCColumn("eventmasterid")
    val eventMasterId: String? = null,

    @Column(name = "completeworkyn", length = 18)
    @HCColumn("completeworkyn")
    val completeWorkYn: String? = null

    // Phase2: 기존 V2 필드 주석 처리
    // val id: Long = 0,                    // auto-increment PK → 복합 키로 대체
    // val userId: Long,                    // → employeeId (String)로 대체
    // val submissionDate: LocalDate,       // → workingDate로 대체
    // val submittedAt: LocalDateTime,      // → completeTime으로 대체
    // @UniqueConstraint(name = "uq_safety_check_user_date", columnNames = ["user_id", "submission_date"])
)
// Phase2: addItem 메서드 제거 — SafetyCheckSubmissionItem 주석 처리됨
