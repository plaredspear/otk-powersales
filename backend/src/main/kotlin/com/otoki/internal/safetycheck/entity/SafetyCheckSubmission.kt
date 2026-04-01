package com.otoki.internal.safetycheck.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "safety_check_submission",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_safety_check_employee_date_schedule",
            columnNames = ["employee_id", "working_date", "display_work_schedule_id"]
        )
    ]
)
@HCTable("safetycheck__workschedule__member")
class SafetyCheckSubmission(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "safety_check_submission_id")
    val id: Long = 0,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @HCColumn("employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @HCColumn("working__date")
    @Column(name = "working_date")
    val workingDate: LocalDate? = null,

    @Column(name = "display_work_schedule_id")
    val displayWorkScheduleId: Long? = null,

    @HCColumn("masterId")
    @Column(name = "display_work_schedule_sfid", length = 18)
    val displayWorkScheduleSfid: String? = null,

    @HCColumn("starttime")
    @Column(name = "start_time")
    val startTime: LocalDateTime? = null,

    @HCColumn("completetime")
    @Column(name = "complete_time")
    val completeTime: LocalDateTime? = null,

    @HCColumn("yes_chkcnt")
    @Column(name = "yes_check_count")
    val yesCheckCount: Int? = null,

    @HCColumn("no_chkcnt")
    @Column(name = "no_check_count")
    val noCheckCount: Int? = null,

    @HCColumn("equipment1")
    @Column(name = "equipment1", length = 10)
    val equipment1: String? = null,

    @HCColumn("equipment2")
    @Column(name = "equipment2", length = 10)
    val equipment2: String? = null,

    @HCColumn("equipment3")
    @Column(name = "equipment3", length = 10)
    val equipment3: String? = null,

    @HCColumn("equipment4")
    @Column(name = "equipment4", length = 10)
    val equipment4: String? = null,

    @HCColumn("equipment5")
    @Column(name = "equipment5", length = 10)
    val equipment5: String? = null,

    @HCColumn("equipment6")
    @Column(name = "equipment6", length = 10)
    val equipment6: String? = null,

    @HCColumn("equipment7")
    @Column(name = "equipment7", length = 10)
    val equipment7: String? = null,

    @HCColumn("equipment8")
    @Column(name = "equipment8", length = 10)
    val equipment8: String? = null,

    @HCColumn("equipment9")
    @Column(name = "equipment9", length = 10)
    val equipment9: String? = null,

    @HCColumn("precaution")
    @Column(name = "precaution", length = 3000)
    val precaution: String? = null,

    @HCColumn("precaution_chkcnt")
    @Column(name = "precaution_check_count")
    val precautionCheckCount: Int? = null,

    @HCColumn("traversalflag")
    @Column(name = "traversal_flag", length = 255)
    val traversalFlag: String? = null,

    @Column(name = "team_member_schedule_id")
    val teamMemberScheduleId: Long? = null,

    @HCColumn("eventmasterid")
    @Column(name = "team_member_schedule_sfid", length = 18)
    val teamMemberScheduleSfid: String? = null,

    @HCColumn("completeworkyn")
    @Column(name = "complete_work_yn", length = 18)
    var completeWorkYn: String? = null,

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "display_work_schedule_id", insertable = false, updatable = false)
    val displayWorkSchedule: DisplayWorkSchedule? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_member_schedule_id", insertable = false, updatable = false)
    val teamMemberSchedule: TeamMemberSchedule? = null
) : BaseEntity()
