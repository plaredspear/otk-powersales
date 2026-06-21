package com.otoki.powersales.domain.activity.safetycheck.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@DomainName("안전점검제출")
@Entity
// 레거시 Heroku 원본(safetycheck__workschedule__member)에 PK/unique 제약이 없어 같은 직원·날짜·
// 일정 복수 안전점검 row 를 허용했으므로, 신규에서도 unique 제약을 두지 않는다 (제약 제거:
// V202606151000__drop_safety_check_submission_unique.sql). 중복 방지가 필요하면 앱 레벨에서 처리.
@Table(name = "safety_check_submission")
@HerokuOnly("safetycheck__workschedule__member")
class SafetyCheckSubmission(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("안전점검제출ID")
    @Column(name = "safety_check_submission_id")
    val id: Long = 0,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @HCColumn("employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @HCColumn("working__date")
    @FieldName("영업관리일")
    @Column(name = "working_date")
    val workingDate: LocalDate? = null,

    @FieldName("진열근무일정ID")
    @Column(name = "display_work_schedule_id")
    val displayWorkScheduleId: Long? = null,

    @HCColumn("masterId")
    @Column(name = "display_work_schedule_sfid", length = 18)
    val displayWorkScheduleSfid: String? = null,

    @HCColumn("starttime")
    @FieldName("점검시간")
    @Column(name = "start_time")
    val startTime: LocalDateTime? = null,

    @HCColumn("completetime")
    @FieldName("완료시간")
    @Column(name = "complete_time")
    val completeTime: LocalDateTime? = null,

    @HCColumn("yes_chkcnt")
    @FieldName("예체크건수")
    @Column(name = "yes_check_count")
    val yesCheckCount: Int? = null,

    @HCColumn("no_chkcnt")
    @FieldName("아니오체크건수")
    @Column(name = "no_check_count")
    val noCheckCount: Int? = null,

    @HCColumn("equipment1")
    @FieldName("1) 손목보호대를 착용했습니다.")
    @Column(name = "equipment1", length = 10)
    val equipment1: String? = null,

    @HCColumn("equipment2")
    @FieldName("2) 숨수건(화재피해 예방)을 소지하고 있습니다.")
    @Column(name = "equipment2", length = 10)
    val equipment2: String? = null,

    @HCColumn("equipment3")
    @FieldName("3) 안전화를 착용했습니다.")
    @Column(name = "equipment3", length = 10)
    val equipment3: String? = null,

    @HCColumn("equipment4")
    @FieldName("4) 진열업무시 코팅장갑 및 허리보호대를 착용합니다.")
    @Column(name = "equipment4", length = 10)
    val equipment4: String? = null,

    @HCColumn("equipment5")
    @FieldName("5) 진열대가 높을 경우 안전사다리를 사용합니다.")
    @Column(name = "equipment5", length = 10)
    val equipment5: String? = null,

    @HCColumn("equipment6")
    @FieldName("6) 시식행사 진행시 위생장갑을 사용합니다.")
    @Column(name = "equipment6", length = 10)
    val equipment6: String? = null,

    @HCColumn("equipment7")
    @FieldName("7) 오뚜기 유니폼을 착용하였습니다.")
    @Column(name = "equipment7", length = 10)
    val equipment7: String? = null,

    @HCColumn("equipment8")
    @FieldName("8) 오뚜기 판매여사원 명찰을 착용하였습니다.")
    @Column(name = "equipment8", length = 10)
    val equipment8: String? = null,

    @HCColumn("equipment9")
    @FieldName("9) 코로나 예방 마스크 착용 했습니다.")
    @Column(name = "equipment9", length = 10)
    val equipment9: String? = null,

    @HCColumn("precaution")
    @FieldName("예방사항 통합관리")
    @Column(name = "precaution", length = 3000)
    val precaution: String? = null,

    @HCColumn("precaution_chkcnt")
    @FieldName("주의사항체크건수")
    @Column(name = "precaution_check_count")
    val precautionCheckCount: Int? = null,

    @HCColumn("traversalflag")
    @FieldName("순회체크")
    @Column(name = "traversal_flag", length = 255)
    val traversalFlag: String? = null,

    @FieldName("여사원일정ID")
    @Column(name = "team_member_schedule_id")
    val teamMemberScheduleId: Long? = null,

    @HCColumn("eventmasterid")
    @Column(name = "team_member_schedule_sfid", length = 18)
    val teamMemberScheduleSfid: String? = null,

    @HCColumn("completeworkyn")
    @FieldName("작업완료여부")
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
