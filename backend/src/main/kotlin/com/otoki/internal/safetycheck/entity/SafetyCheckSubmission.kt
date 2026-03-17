package com.otoki.internal.safetycheck.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "safety_check_submission")
@HCTable("safetycheck__workschedule__member")
@IdClass(SafetyCheckMemberId::class)
class SafetyCheckSubmission(

    @Id
    @Column(name = "master_id", length = 18)
    @HCColumn("masterId")
    val masterId: String = "",

    @Id
    @Column(name = "employee_id", length = 18)
    @HCColumn("employeeid__c")
    val employeeId: String = "",

    @Id
    @Column(name = "working_date")
    @HCColumn("working__date")
    val workingDate: LocalDate? = null,

    @Column(name = "start_time")
    @HCColumn("starttime")
    val startTime: LocalDateTime? = null,

    @Column(name = "complete_time")
    @HCColumn("completetime")
    val completeTime: LocalDateTime? = null,

    @Column(name = "yes_check_count")
    @HCColumn("yes_chkcnt")
    val yesCheckCount: Int? = null,

    @Column(name = "no_check_count")
    @HCColumn("no_chkcnt")
    val noCheckCount: Int? = null,

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

    @Column(name = "precaution_check_count")
    @HCColumn("precaution_chkcnt")
    val precautionCheckCount: Int? = null,

    @Column(name = "traversal_flag", length = 255)
    @HCColumn("traversalflag")
    val traversalFlag: String? = null,

    @Column(name = "event_master_id", length = 18)
    @HCColumn("eventmasterid")
    val eventMasterId: String? = null,

    @Column(name = "complete_work_yn", length = 18)
    @HCColumn("completeworkyn")
    val completeWorkYn: String? = null
)
