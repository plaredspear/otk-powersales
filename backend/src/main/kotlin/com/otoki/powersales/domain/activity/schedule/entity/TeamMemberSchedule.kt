package com.otoki.powersales.domain.activity.schedule.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingCategory5
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory1Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory2Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory3Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory5Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingTypeConverter
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.org.leave.entity.AlternativeHoliday
import com.otoki.powersales.domain.activity.promotion.entity.PromotionEmployee
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 일정 Entity
 * V1 스키마: dkretail__teammemberschedule__c (팀원 스케줄 + 안전점검 장비 + 업무보고)
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("여사원일정")
@Entity
@Table(name = "team_member_schedule")
@SFObject("DKRetail__TeamMemberSchedule__c")
class TeamMemberSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("여사원일정ID")
    @Column(name = "team_member_schedule_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__WorkingDate__c")
    @FieldName("근무일자")
    @Column(name = "working_date")
    var workingDate: LocalDate? = null,

    @SFField("DKRetail__WorkingType__c")
    @FieldName("근무형태")
    @Column(name = "working_type", length = 255)
    @Convert(converter = WorkingTypeConverter::class)
    var workingType: WorkingType? = null,

    @SFField("DKRetail__WorkingCategory1__c")
    @FieldName("근무유형1")
    @Column(name = "working_category1", length = 255)
    @Convert(converter = WorkingCategory1Converter::class)
    var workingCategory1: WorkingCategory1? = null,

    @SFField("DKRetail__WorkingCategory2__c")
    @FieldName("근무유형2")
    @Column(name = "working_category2", length = 255)
    @Convert(converter = WorkingCategory2Converter::class)
    val workingCategory2: WorkingCategory2? = null,

    @SFField("DKRetail__WorkingCategory3__c")
    @FieldName("근무유형3")
    @Column(name = "working_category3", length = 255)
    @Convert(converter = WorkingCategory3Converter::class)
    var workingCategory3: WorkingCategory3? = null,

    @SFField("WorkingCategory4__c")
    @FieldName("WorkingCategory4")
    @Column(name = "working_category4", length = 255)
    var workingCategory4: String? = null,

    @SFField("AccountId__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    // -- Spec #849: deprecated SF lookup `DKRetail__AccountId__c` 부활 (raw FK id 컬럼만, @ManyToOne 미도입) --
    @SFField("DKRetail__AccountId__c")
    @Column(name = "dk_account_sfid", length = 18)
    val dkAccountSfid: String? = null,

    @FieldName("거래처ID")
    @Column(name = "dk_account_id")
    val dkAccountId: Long? = null,

    /**
     * 조장(팀리더) sfid sync buffer.
     * SF prod 메타가 `type=string(100)` 자유 텍스트로 정의되어 있으므로 SF 라이브 권위 정합 (§6.8) 으로 entity length=100 적용 — 절단 위험 회피.
     * 실 운영값은 SF Id 18자 고정으로 추정되나 SF org 메타가 정비되기 전까지는 SF 정합 우선 (추후 SF org length=18 좁힘 시 entity 도 동반 축소 예정).
     */
    @SFField("teamleadersfid__c")
    @Column(name = "team_leader_sfid", length = 100)
    val teamLeaderSfid: String? = null,

    @SFField("DKRetail__AltHolidayId__c")
    @Column(name = "alt_holiday_sfid", length = 18)
    val altHolidaySfid: String? = null,

    @SFField("DKRetail__CommuteLogId__c")
    @Column(name = "commute_log_sfid", length = 18)
    var commuteLogSfid: String? = null,

    @SFField("DKRetail__PromotionEmpId__c")
    @Column(name = "promotion_employee_sfid", length = 18)
    val promotionEmployeeSfid: String? = null,

    /**
     * 진열 마스터(`DKRetail__DisplayWorkScheduleMaster__c`) 연결 sfid (Spec #587 P1-B).
     * 진열 출근 시 마스터의 sfid 를 그대로 카피. 마이그레이션 후 display_work_schedule_id 가 채워진다.
     */
    @SFField("DisplayWorkScheduleMaster__c")
    @Column(name = "display_work_schedule_sfid", length = 18)
    val displayWorkScheduleSfid: String? = null,

    @SFField("CommuteReportDateTime__c")
    @FieldName("CommuteReportDateTime")
    @Column(name = "commute_report_datetime")
    val commuteReportDatetime: LocalDateTime? = null,

    @SFField("ID__c")
    @FieldName("ID")
    @Column(name = "id_field", length = 30)
    val idField: String? = null,

    @SFField("TraversalFlag__c")
    @FieldName("순회체크")
    @Column(name = "traversal_flag", length = 10)
    var traversalFlag: String? = null,

    @SFField("Equipment1__c")
    @FieldName("1) 손목보호대를 착용했습니다.")
    @Column(name = "equipment1", length = 10)
    var equipment1: String? = null,

    @SFField("Equipment2__c")
    @FieldName("2) 숨수건(화재피해 예방)을 소지하고 있습니다.")
    @Column(name = "equipment2", length = 10)
    var equipment2: String? = null,

    @SFField("Equipment3__c")
    @FieldName("3) 안전화를 착용했습니다.")
    @Column(name = "equipment3", length = 10)
    var equipment3: String? = null,

    @SFField("Equipment4__c")
    @FieldName("4) 진열업무시 코팅장갑 및 허리보호대를 착용합니다.")
    @Column(name = "equipment4", length = 10)
    var equipment4: String? = null,

    @SFField("Equipment5__c")
    @FieldName("5) 진열대가 높을 경우 안전사다리를 사용합니다.")
    @Column(name = "equipment5", length = 10)
    var equipment5: String? = null,

    @SFField("Equipment6__c")
    @FieldName("6) 시식행사 진행시 위생장갑을 사용합니다.")
    @Column(name = "equipment6", length = 10)
    var equipment6: String? = null,

    @SFField("Equipment7__c")
    @FieldName("7) 오뚜기 유니폼을 착용하였습니다.")
    @Column(name = "equipment7", length = 10)
    var equipment7: String? = null,

    @SFField("Equipment8__c")
    @FieldName("8) 오뚜기 판매여사원 명찰을 착용하였습니다.")
    @Column(name = "equipment8", length = 10)
    var equipment8: String? = null,

    @SFField("Equipment9__c")
    @FieldName("9) 코로나 예방 마스크 착용 했습니다.")
    @Column(name = "equipment9", length = 10)
    var equipment9: String? = null,

    @SFField("Equipment10__c")
    @FieldName("10) 확장1")
    @Column(name = "equipment10", length = 10)
    val equipment10: String? = null,

    @SFField("Yes_ChkCnt__c")
    @FieldName("&lt;예&gt; 체크 개수")
    @Column(name = "yes_chk_cnt")
    var yesChkCnt: Double? = null,

    @SFField("No_ChkCnt__c")
    @FieldName("&lt;해당없음&gt; 체크 개수")
    @Column(name = "no_chk_cnt")
    var noChkCnt: Double? = null,

    @SFField("precaution_chk__c")
    @FieldName("예방사항 총 체크")
    @Column(name = "precaution_chk")
    var precautionChk: Double? = null,

    @SFField("precaution__c")
    @FieldName("예방사항 통합관리")
    @Column(name = "precaution", length = 3000)
    var precaution: String? = null,

    @SFField("StartTime__c")
    @FieldName("점검시간")
    @Column(name = "start_time")
    var startTime: LocalDateTime? = null,

    @SFField("CompleteTime__c")
    @FieldName("완료시간")
    @Column(name = "complete_time")
    var completeTime: LocalDateTime? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Spec #609: SF 누락 컬럼 7개 신규 도입 (Q1 옵션 1) --

    @SFField("HRCode__c")
    @FieldName("HRCode")
    @Column(name = "hr_code", length = 255)
    var hrCode: String? = null,

    @SFField("DKRetail__PromotionEmpIdExt__c")
    @FieldName("PromotionEmpIdExt")
    @Column(name = "promotion_emp_id_ext", length = 30)
    var promotionEmpIdExt: String? = null,

    /**
     * SF `SecondWorkType__c` 는 type=string(255) free-form (picklist 아님) — Spec #762.
     * 다른 entity (AttendanceLog / DisplayWorkSchedule) 의 동명 enum 필드 (`DKRetail__SecondWorkType__c`, picklist) 와 SF API Name 이 다른 별개 필드.
     * 본 entity 한정으로 enum 매핑 환원 — Converter 부착 금지.
     */
    @SFField("SecondWorkType__c")
    @FieldName("근무유형4")
    @Column(name = "second_work_type", length = 255)
    var secondWorkType: String? = null,

    @SFField("WorkingCategory5__c")
    @FieldName("근무유형5")
    @Column(name = "working_category5", length = 255)
    @Convert(converter = WorkingCategory5Converter::class)
    var workingCategory5: WorkingCategory5? = null,

    @SFField("ref_accountName__c")
    @FieldName("참조용거래처명")
    @Column(name = "ref_account_name", length = 255)
    var refAccountName: String? = null,

    @SFField("MonthlyFemaleEmployeeIntegrationSchedule__c")
    @Column(name = "monthly_female_employee_integration_schedule_sfid", length = 18)
    var monthlyFemaleEmployeeIntegrationScheduleSfid: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @FieldName("전문행사조")
    @Column(name = "professional_promotion_team", length = 255)
    var professionalPromotionTeam: String? = null,

    @SFField("CostCenterCode__c")
    @FieldName("조직유형")
    @Column(name = "cost_center_code", length = 255)
    var costCenterCode: String? = null,

    /**
     * 대리 등록자(조장) employee_id. 조장이 본인 팀원의 일정을 대리 등록할 때 audit trail 용도로 저장.
     * 본 스펙 외부 INSERT(스케줄러 자동 생성 등)는 NULL.
     */
    @FieldName("대리등록자ID")
    @Column(name = "proxy_registered_by")
    val proxyRegisteredBy: Long? = null,

    // -- Group A R-2: Owner polymorphic [Group, User] / CreatedBy User / LastModifiedBy User --

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    var employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_leader_id")
    val teamLeader: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alt_holiday_id")
    val altHoliday: AlternativeHoliday? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_employee_id")
    var promotionEmployee: PromotionEmployee? = null,

    /**
     * 진열 마스터 (Spec #587 P1-B). 진열 출근 케이스에서 채워진다.
     * FK 제약은 본 스펙 비범위 (spec.md §6.2 — 후속 스펙에서 추가 예정).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "display_work_schedule_id")
    var displayWorkSchedule: DisplayWorkSchedule? = null,

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

    // -- Spec #746 R-2 (MonthlyFemaleEmployeeIntegrationSchedule__c FK 신설) --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "monthly_female_employee_integration_schedule_id")
    var monthlyFemaleEmployeeIntegrationSchedule: MonthlyFemaleEmployeeIntegrationSchedule? = null,

    // -- Spec #749 R-2 (DKRetail__CommuteLogId__c FK 신설 — AttendanceLog) --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_log_id")
    var attendanceLog: AttendanceLog? = null,

    ) : BaseEntity() {

    // -- Spec #762 Formula 컬럼 7건 computed property 재현 (§6.7 entity 컬럼 추가 금지 정책 정합) --

    /** SF formula `DKRetail__ActualWorkDate__c` 동등 — 대휴 원본 근무일자. */
    val actualWorkDate: LocalDate?
        get() = altHoliday?.actualWorkDate

    /** SF formula `DKRetail__CommuteDate__c` 동등 — 출퇴근 로그의 출근일시. */
    val commuteDate: LocalDateTime?
        get() = attendanceLog?.attendanceDate

    /** SF formula `DKRetail__ConfirmAltHolidayDate__c` 동등 — 대휴 확정일자. */
    val confirmAltHolidayDate: LocalDate?
        get() = altHoliday?.confirmAltHolidayDate

    /** SF formula `DKRetail__Day__c` 동등 — 근무일자의 일 (1~31). */
    val dkDay: Int?
        get() = workingDate?.dayOfMonth

    /** SF formula `DKRetail__Reason__c` 동등 — 출퇴근 로그의 사유. */
    val reason: String?
        get() = attendanceLog?.reason

    /** SF formula `DKRetail__SecondWorkType__c` (TEXT(picklist)) 동등 — 출퇴근 로그의 부가 근무유형 표시값. */
    val secondWorkTypeText: String?
        get() = attendanceLog?.secondWorkType?.displayName

    /** SF formula `isworkreport__c` 동등 — 출퇴근 로그 존재 시 "근무등록", 부재 시 빈 문자열. */
    val isWorkReport: String
        get() = if (attendanceLog != null) "근무등록" else ""

    fun updateForPromotion(
        employee: Employee,
        account: Account,
        workingDate: LocalDate,
        workingType: WorkingType,
        workingCategory1: WorkingCategory1,
        workingCategory3: WorkingCategory3,
        workingCategory4: String?,
        promotionEmployee: PromotionEmployee
    ) {
        this.employee = employee
        this.account = account
        this.workingDate = workingDate
        this.workingType = workingType
        this.workingCategory1 = workingCategory1
        this.workingCategory3 = workingCategory3
        this.workingCategory4 = workingCategory4
        this.promotionEmployee = promotionEmployee
    }
}
